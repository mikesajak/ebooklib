package com.mikesajak.ebooklib.importing.application.services

import com.mikesajak.ebooklib.importing.application.ports.incoming.GroupUploadUseCase
import com.mikesajak.ebooklib.importing.application.ports.outgoing.ResolutionItemRepositoryPort
import com.mikesajak.ebooklib.importing.application.ports.outgoing.StagedEbookUploadRepositoryPort
import com.mikesajak.ebooklib.importing.domain.model.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
class GroupingService(
    private val resolutionItemRepository: ResolutionItemRepositoryPort,
    private val stagedUploadRepository: StagedEbookUploadRepositoryPort
) : GroupUploadUseCase {

    private val sessionLocks = ConcurrentHashMap<UUID, Any>()

    @Transactional
    override fun group(
        uploadId: StagedEbookUploadId,
        title: String,
        authors: List<String>,
        fileName: String
    ): ResolutionItemId? {
        val upload = stagedUploadRepository.findById(uploadId) ?: throw IllegalArgumentException("Upload $uploadId not found")
        val sessionId = upload.importSessionId ?: return null

        val lock = sessionLocks.computeIfAbsent(sessionId.value) { Any() }

        synchronized(lock) {
            val normalizedTitle = normalize(title)
            val normalizedAuthors = authors.map { normalize(it) }.sorted()
            val fileNameStem = normalize(ImportUtils.extractTitleFromFileName(fileName))

            val sessionItems = resolutionItemRepository.findByImportSessionId(sessionId)

            // Search for existing ResolutionItem in the same session by title/author/filename stem similarity
            val existing = sessionItems.find { item ->
                val itemTitleNorm = normalize(item.title)
                val itemAuthorsNorm = item.authors.map { normalize(it) }.sorted()

                val titleMatches = itemTitleNorm.isNotBlank() && !ImportUtils.isUnlikelyTitle(itemTitleNorm) && (
                        itemTitleNorm == normalizedTitle ||
                        itemTitleNorm == fileNameStem ||
                        (normalizedTitle.isNotBlank() && !ImportUtils.isUnlikelyTitle(normalizedTitle) && (itemTitleNorm.contains(normalizedTitle) || normalizedTitle.contains(itemTitleNorm)))
                )

                val authorsMatch = itemAuthorsNorm == normalizedAuthors ||
                        itemAuthorsNorm.isEmpty() ||
                        normalizedAuthors.isEmpty()

                val formatUploads = stagedUploadRepository.findByResolutionItemId(item.id.value)
                val filenameStemMatches = formatUploads.any { existingUpload ->
                    normalize(ImportUtils.extractTitleFromFileName(existingUpload.fileName)) == fileNameStem
                }

                (titleMatches && authorsMatch) || filenameStemMatches
            }

            return if (existing != null) {
                if (existing.authors.isEmpty() && authors.isNotEmpty()) {
                    resolutionItemRepository.save(existing.copy(authors = authors, updatedAt = Instant.now()))
                }
                val updatedUpload = upload.copy(resolutionItemId = existing.id.value)
                stagedUploadRepository.save(updatedUpload)
                existing.id
            } else {
                val effectiveTitle = if (title.isNotBlank() && !ImportUtils.isUnlikelyTitle(title)) title else ImportUtils.extractTitleFromFileName(fileName)

                val newItem = ResolutionItem(
                    id = ResolutionItemId(UUID.randomUUID()),
                    importSessionId = sessionId,
                    title = effectiveTitle,
                    authors = authors,
                    status = ResolutionItemStatus.UNRESOLVED,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
                resolutionItemRepository.save(newItem)

                val updatedUpload = upload.copy(resolutionItemId = newItem.id.value)
                stagedUploadRepository.save(updatedUpload)

                newItem.id
            }
        }
    }

    private fun normalize(s: String): String = s.lowercase(Locale.getDefault()).trim()
}
