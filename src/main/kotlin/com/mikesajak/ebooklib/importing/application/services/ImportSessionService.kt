package com.mikesajak.ebooklib.importing.application.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileStoragePort
import com.mikesajak.ebooklib.importing.application.ports.incoming.ImportSessionUseCase
import com.mikesajak.ebooklib.importing.application.ports.outgoing.ImportSessionRepositoryPort
import com.mikesajak.ebooklib.importing.application.ports.outgoing.ResolutionItemRepositoryPort
import com.mikesajak.ebooklib.importing.application.ports.outgoing.StagedEbookUploadRepositoryPort
import com.mikesajak.ebooklib.importing.domain.model.ImportSession
import com.mikesajak.ebooklib.importing.domain.model.ImportSessionId
import com.mikesajak.ebooklib.importing.domain.model.ImportSessionStatus
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUpload
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

private val logger = KotlinLogging.logger {}

@Service
class ImportSessionService(
    private val repository: ImportSessionRepositoryPort,
    private val stagedUploadRepository: StagedEbookUploadRepositoryPort,
    private val resolutionItemRepository: ResolutionItemRepositoryPort,
    private val fileStoragePort: FileStoragePort,
    private val objectMapper: ObjectMapper
) : ImportSessionUseCase {

    @Transactional
    override fun createSession(totalFiles: Int): ImportSession {
        val now = Instant.now()
        val session = ImportSession(
            id = ImportSessionId(UUID.randomUUID()),
            status = ImportSessionStatus.ACTIVE,
            totalFiles = totalFiles,
            processedFiles = 0,
            failedFiles = 0,
            createdAt = now,
            updatedAt = now,
            expiryAt = now.plus(24, ChronoUnit.HOURS)
        )
        return repository.save(session)
    }

    override fun getSession(id: ImportSessionId): ImportSession? {
        return repository.findById(id)
    }

    override fun getActiveSessions(): List<ImportSession> {
        return repository.findAllByStatus(ImportSessionStatus.ACTIVE) + 
               repository.findAllByStatus(ImportSessionStatus.PROCESSING)
    }

    @Transactional
    override fun updateProgress(id: ImportSessionId, processed: Int, failed: Int): ImportSession {
        val session = repository.findById(id) ?: throw IllegalArgumentException("Session $id not found")
        val updated = session.copy(
            processedFiles = processed,
            failedFiles = failed,
            updatedAt = Instant.now()
        )
        return repository.save(updated)
    }

    @Transactional
    override fun finalizeSession(id: ImportSessionId): ImportSession {
        val session = repository.findById(id) ?: throw IllegalArgumentException("Session $id not found")
        val updated = session.copy(
            status = ImportSessionStatus.FINALIZED,
            updatedAt = Instant.now()
        )
        return repository.save(updated)
    }

    @Transactional
    override fun cancelSession(id: ImportSessionId): ImportSession {
        val session = repository.findById(id) ?: throw IllegalArgumentException("Session $id not found")
        val updated = session.copy(
            status = ImportSessionStatus.CANCELLED,
            updatedAt = Instant.now()
        )
        return repository.save(updated)
    }

    @Transactional
    override fun deleteSession(id: ImportSessionId) {
        logger.info { "Deleting import session $id and all associated data" }
        
        // 1. Cleanup files from storage
        val associatedUploads = stagedUploadRepository.findByImportSessionId(id)
        associatedUploads.forEach { upload ->
            cleanupUpload(upload)
        }

        // 2. Delete ResolutionItems
        resolutionItemRepository.deleteByImportSessionId(id)

        // 3. Delete StagedUploads
        stagedUploadRepository.deleteByImportSessionId(id)

        // 4. Delete Session
        repository.delete(id)
    }

    private fun cleanupUpload(upload: StagedEbookUpload) {
        try {
            fileStoragePort.deleteFile("staged/${upload.id}")
            
            val metadataMap = upload.metadataJson?.let {
                try {
                    objectMapper.readValue(it, Map::class.java)
                } catch (e: Exception) { null }
            }
            
            val coverStorageKey = metadataMap?.get("coverStorageKey") as? String
            if (coverStorageKey != null) {
                fileStoragePort.deleteFile(coverStorageKey)
            }
        } catch (e: Exception) {
            logger.warn { "Failed to cleanup storage for upload ${upload.id}: ${e.message}" }
        }
    }
}
