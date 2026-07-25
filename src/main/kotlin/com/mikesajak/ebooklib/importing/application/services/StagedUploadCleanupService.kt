package com.mikesajak.ebooklib.importing.application.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileStoragePort
import com.mikesajak.ebooklib.importing.application.ports.outgoing.StagedEbookUploadRepositoryPort
import com.mikesajak.ebooklib.importing.application.ports.incoming.StagedUploadCleanupUseCase
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUpload
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import jakarta.transaction.Transactional
import java.time.Instant

private val logger = KotlinLogging.logger {}

@Service
class StagedUploadCleanupService(
    private val repository: StagedEbookUploadRepositoryPort,
    private val fileStoragePort: FileStoragePort,
    private val objectMapper: ObjectMapper
) : StagedUploadCleanupUseCase {

    @Scheduled(cron = "\${app.import.cleanup.cron:0 0 * * * *}")
    @Transactional
    override fun cleanupExpiredUploads(): Int {
        val now = Instant.now()
        logger.info { "Starting cleanup of expired staged uploads (now: $now)" }

        val expiredUploads = repository.findByExpiryAtBefore(now)
        logger.info { "Found ${expiredUploads.size} expired uploads to clean up" }

        var deletedCount = 0
        expiredUploads.forEach { upload ->
            try {
                cleanupUpload(upload)
                deletedCount++
            } catch (e: Exception) {
                logger.error(e) { "Failed to clean up expired upload ${upload.id}" }
            }
        }

        logger.info { "Finished cleanup of expired staged uploads. Deleted: $deletedCount" }
        return deletedCount
    }

    private fun cleanupUpload(upload: StagedEbookUpload) {
        logger.debug { "Cleaning up expired upload: ${upload.id} (expired at: ${upload.expiryAt})" }

        // 1. Delete main ebook file
        try {
            fileStoragePort.deleteFile("staged/${upload.id}")
        } catch (e: Exception) {
            logger.warn { "Failed to delete staged file ${upload.id} from storage: ${e.message}" }
        }

        // 2. Delete cover if present
        val metadataMap = upload.metadataJson?.let {
            @Suppress("UNCHECKED_CAST")
            try {
                objectMapper.readValue(it, Map::class.java) as Map<String, Any?>
            } catch (e: Exception) {
                null
            }
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
        repository.delete(upload.id)
        logger.debug { "Deleted staging record and files for upload ${upload.id}" }
    }
}
