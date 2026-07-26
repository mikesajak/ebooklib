package com.mikesajak.ebooklib.importing.application.services

import com.mikesajak.ebooklib.file.application.ports.outgoing.FileStoragePort
import com.mikesajak.ebooklib.importing.application.ports.incoming.ResolutionItemUseCase
import com.mikesajak.ebooklib.importing.application.ports.outgoing.ResolutionItemRepositoryPort
import com.mikesajak.ebooklib.importing.domain.model.ImportSessionId
import com.mikesajak.ebooklib.importing.domain.model.ResolutionItem
import com.mikesajak.ebooklib.importing.domain.model.ResolutionItemId
import com.mikesajak.ebooklib.importing.domain.model.ResolutionItemStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

import com.mikesajak.ebooklib.importing.application.ports.outgoing.StagedEbookUploadRepositoryPort
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadId
import java.util.*

@Service
class ResolutionItemService(
    private val repository: ResolutionItemRepositoryPort,
    private val stagedUploadRepository: StagedEbookUploadRepositoryPort,
    private val fileStoragePort: FileStoragePort
) : ResolutionItemUseCase {

    override fun getResolutionItems(sessionId: ImportSessionId): List<ResolutionItem> {
        return repository.findByImportSessionId(sessionId)
    }

    override fun getResolutionItem(id: ResolutionItemId): ResolutionItem? {
        return repository.findById(id)
    }

    @Transactional
    override fun updateStatus(id: ResolutionItemId, status: ResolutionItemStatus): ResolutionItem {
        val item = repository.findById(id)
        if (item != null) {
            val updated = item.copy(
                status = status,
                updatedAt = Instant.now()
            )
            return repository.save(updated)
        }

        val upload = stagedUploadRepository.findById(StagedEbookUploadId(id.value))
            ?: throw IllegalArgumentException("ResolutionItem or StagedUpload $id not found")

        val newItem = ResolutionItem(
            id = id,
            importSessionId = upload.importSessionId ?: throw IllegalStateException("Upload $id has no session"),
            title = upload.fileName.substringBeforeLast('.'),
            authors = emptyList(),
            status = status,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val savedItem = repository.save(newItem)
        stagedUploadRepository.save(upload.copy(resolutionItemId = savedItem.id.value))
        return savedItem
    }

    @Transactional
    override fun updateMetadata(id: ResolutionItemId, metadataJson: String?): ResolutionItem {
        val item = repository.findById(id) ?: throw IllegalArgumentException("ResolutionItem $id not found")
        val updated = item.copy(
            metadataJson = metadataJson,
            updatedAt = Instant.now()
        )
        return repository.save(updated)
    }

    @Transactional
    override fun updateResolvedItem(
        id: ResolutionItemId,
        title: String,
        authors: List<String>,
        status: ResolutionItemStatus,
        metadataJson: String?
    ): ResolutionItem {
        val item = repository.findById(id) ?: throw IllegalArgumentException("ResolutionItem $id not found")
        val updated = item.copy(
            title = title,
            authors = authors,
            status = status,
            metadataJson = metadataJson,
            updatedAt = Instant.now()
        )
        return repository.save(updated)
    }

    @Transactional
    override fun bulkUpdateStatus(ids: List<ResolutionItemId>, status: ResolutionItemStatus) {
        val now = Instant.now()
        ids.forEach { id ->
            repository.findById(id)?.let { item ->
                repository.save(item.copy(status = status, updatedAt = now))
            }
        }
    }

    @Transactional
    override fun detachFormat(uploadId: StagedEbookUploadId): ResolutionItem {
        val upload = stagedUploadRepository.findById(uploadId)
            ?: throw IllegalArgumentException("Staged upload $uploadId not found")

        val currentResolutionItemId = upload.resolutionItemId
        if (currentResolutionItemId != null) {
            val siblingUploads = stagedUploadRepository.findByResolutionItemId(currentResolutionItemId)
            if (siblingUploads.size <= 1) {
                // Already the only upload in its resolution item
                return repository.findById(ResolutionItemId(currentResolutionItemId))!!
            }
        }

        val newItem = ResolutionItem(
            id = ResolutionItemId(UUID.randomUUID()),
            importSessionId = upload.importSessionId ?: throw IllegalStateException("Upload $uploadId has no session"),
            title = upload.fileName.substringBeforeLast('.'),
            authors = emptyList(),
            status = ResolutionItemStatus.UNRESOLVED,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val savedNewItem = repository.save(newItem)

        val updatedUpload = upload.copy(resolutionItemId = savedNewItem.id.value)
        stagedUploadRepository.save(updatedUpload)

        return savedNewItem
    }

    @Transactional
    override fun mergeItems(primaryItemId: ResolutionItemId, sourceItemIds: List<ResolutionItemId>): ResolutionItem {
        val primaryItem = repository.findById(primaryItemId)
            ?: throw IllegalArgumentException("Primary ResolutionItem $primaryItemId not found")

        var updatedAuthors = primaryItem.authors

        sourceItemIds.distinct().forEach { sourceId ->
            if (sourceId != primaryItemId) {
                val sourceItem = repository.findById(sourceId)
                if (sourceItem != null) {
                    if (updatedAuthors.isEmpty() && sourceItem.authors.isNotEmpty()) {
                        updatedAuthors = sourceItem.authors
                    }
                    val uploadsToMove = stagedUploadRepository.findByResolutionItemId(sourceId.value)
                    uploadsToMove.forEach { upload ->
                        stagedUploadRepository.save(upload.copy(resolutionItemId = primaryItemId.value))
                    }
                    repository.delete(sourceId)
                }
            }
        }

        val updatedPrimary = primaryItem.copy(
            authors = updatedAuthors,
            updatedAt = Instant.now()
        )
        return repository.save(updatedPrimary)
    }

    @Transactional
    override fun deleteItem(id: UUID) {
        val resItem = repository.findById(ResolutionItemId(id))
        if (resItem != null) {
            val uploads = stagedUploadRepository.findByResolutionItemId(id)
            uploads.forEach { upload ->
                try {
                    fileStoragePort.deleteFile("staged/${upload.id.value}")
                } catch (e: Exception) {
                    // Ignore deletion error from file storage if file was already missing
                }
                stagedUploadRepository.delete(upload.id)
            }
            repository.delete(resItem.id)
        } else {
            val upload = stagedUploadRepository.findById(StagedEbookUploadId(id))
            if (upload != null) {
                try {
                    fileStoragePort.deleteFile("staged/${upload.id.value}")
                } catch (e: Exception) {
                    // Ignore
                }
                stagedUploadRepository.delete(upload.id)
            }
        }
    }
}
