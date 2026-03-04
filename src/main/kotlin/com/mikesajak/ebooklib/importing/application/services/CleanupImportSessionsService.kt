package com.mikesajak.ebooklib.importing.application.services

import com.mikesajak.ebooklib.file.application.ports.outgoing.FileStoragePort
import com.mikesajak.ebooklib.importing.application.ports.incoming.CleanupImportSessionsUseCase
import com.mikesajak.ebooklib.importing.application.ports.outgoing.ImportSessionRepositoryPort
import com.mikesajak.ebooklib.importing.application.ports.outgoing.StagedEbookUploadRepositoryPort
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUpload
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import jakarta.transaction.Transactional
import java.time.Instant
import com.fasterxml.jackson.databind.ObjectMapper

private val logger = KotlinLogging.logger {}

@Service
class CleanupImportSessionsService(
    private val sessionRepository: ImportSessionRepositoryPort,
    private val stagedUploadRepository: StagedEbookUploadRepositoryPort,
    private val fileStoragePort: FileStoragePort,
    private val objectMapper: ObjectMapper
) : CleanupImportSessionsUseCase {

    @Scheduled(cron = "\${app.import.session.cleanup.cron:0 0 * * * *}")
    @Transactional
    override fun cleanup() {
        val now = Instant.now()
        logger.info { "Starting cleanup of expired import sessions (now: $now)" }

        val expiredSessions = sessionRepository.findAllExpired(now)
        logger.info { "Found ${expiredSessions.size} expired import sessions to clean up" }

        expiredSessions.forEach { session ->
            try {
                logger.debug { "Cleaning up expired session: ${session.id} (expired at: ${session.expiryAt})" }
                
                // 1. Find and cleanup all associated staged uploads
                val associatedUploads = stagedUploadRepository.findByImportSessionId(session.id)
                associatedUploads.forEach { upload ->
                    cleanupUpload(upload)
                }

                // 2. Delete the session itself
                sessionRepository.delete(session.id)
                logger.info { "Deleted expired import session ${session.id} and its ${associatedUploads.size} uploads" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to clean up expired import session ${session.id}" }
            }
        }
    }

    private fun cleanupUpload(upload: StagedEbookUpload) {
        // 1. Delete main ebook file
        try {
            fileStoragePort.deleteFile("staged/${upload.id}")
        } catch (e: Exception) {
            logger.warn { "Failed to delete staged file ${upload.id} from storage: ${e.message}" }
        }

        // 2. Delete cover if present
        val metadataMap = try {
            upload.metadataJson?.let {
                objectMapper.readValue(it, Map::class.java)
            }
        } catch (e: Exception) {
            null
        }

        val coverStorageKey = metadataMap?.get("coverStorageKey") as? String
        if (coverStorageKey != null) {
            try {
                fileStoragePort.deleteFile(coverStorageKey)
            } catch (e: Exception) {
                logger.warn { "Failed to delete staged cover $coverStorageKey from storage: ${e.message}" }
            }
        }

        // 3. Delete DB record
        stagedUploadRepository.delete(upload.id)
    }
}
