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
import com.mikesajak.ebooklib.importing.infrastructure.adapters.incoming.rest.ImportRestMapper
import com.mikesajak.ebooklib.notification.application.NotificationService
import com.mikesajak.ebooklib.notification.domain.model.NotificationEvent
import com.mikesajak.ebooklib.notification.domain.model.NotificationType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
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
    private val objectMapper: ObjectMapper,
    private val notificationService: NotificationService,
    private val importRestMapper: ImportRestMapper
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
        val saved = repository.save(session)
        notificationService.broadcast(NotificationEvent(NotificationType.IMPORT_PROGRESS, importRestMapper.toResponse(saved)))
        return saved
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
        val saved = repository.save(updated)
        notificationService.broadcast(NotificationEvent(NotificationType.IMPORT_PROGRESS, importRestMapper.toResponse(saved)))
        return saved
    }

    @Transactional
    override fun finalizeSession(id: ImportSessionId): ImportSession {
        val session = repository.findById(id) ?: throw IllegalArgumentException("Session $id not found")
        
        logger.info { "Finalizing import session $id. Cleaning up remaining non-resolved data." }

        // 1. Find all remaining staged uploads for this session
        // Resolved ones were already deleted during individual finalization
        val remainingUploads = stagedUploadRepository.findByImportSessionId(id)
        logger.info { "Found ${remainingUploads.size} remaining staged uploads to cleanup in session $id" }
        
        remainingUploads.forEach { upload ->
            cleanupUpload(upload)
        }

        // 2. Delete all staged upload records for this session
        stagedUploadRepository.deleteByImportSessionId(id)

        // 3. Delete all resolution items for this session
        resolutionItemRepository.deleteByImportSessionId(id)

        // 4. Update session status
        val updated = session.copy(
            status = ImportSessionStatus.FINALIZED,
            updatedAt = Instant.now()
        )
        val saved = repository.save(updated)
        notificationService.broadcast(NotificationEvent(NotificationType.IMPORT_PROGRESS, importRestMapper.toResponse(saved)))
        return saved
    }

    @Transactional
    override fun cancelSession(id: ImportSessionId): ImportSession {
        val session = repository.findById(id) ?: throw IllegalArgumentException("Session $id not found")
        val updated = session.copy(
            status = ImportSessionStatus.CANCELLED,
            updatedAt = Instant.now()
        )
        val saved = repository.save(updated)
        notificationService.broadcast(NotificationEvent(NotificationType.IMPORT_PROGRESS, importRestMapper.toResponse(saved)))
        return saved
    }

    @Transactional
    override fun deleteSession(id: ImportSessionId) {
        logger.info { "Deleting import session $id and all associated data" }
        
        // 1. Cleanup files from storage
        val associatedUploads = stagedUploadRepository.findByImportSessionId(id)
        associatedUploads.forEach { upload ->
            cleanupUpload(upload)
        }

        // 2. Delete StagedUploads
        stagedUploadRepository.deleteByImportSessionId(id)

        // 3. Delete ResolutionItems
        resolutionItemRepository.deleteByImportSessionId(id)

        // 4. Delete Session
        val session = repository.findById(id)
        repository.delete(id)
        session?.let {
            notificationService.broadcast(NotificationEvent(NotificationType.IMPORT_PROGRESS, importRestMapper.toResponse(it.copy(status = ImportSessionStatus.CANCELLED))))
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun incrementProcessed(id: ImportSessionId): ImportSession {
        repository.incrementProcessed(id)
        val updated = repository.findById(id) ?: throw IllegalArgumentException("Session $id not found")
        notificationService.broadcast(NotificationEvent(NotificationType.IMPORT_PROGRESS, importRestMapper.toResponse(updated)))
        return updated
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun incrementFailed(id: ImportSessionId): ImportSession {
        repository.incrementFailed(id)
        val updated = repository.findById(id) ?: throw IllegalArgumentException("Session $id not found")
        notificationService.broadcast(NotificationEvent(NotificationType.IMPORT_PROGRESS, importRestMapper.toResponse(updated)))
        return updated
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
