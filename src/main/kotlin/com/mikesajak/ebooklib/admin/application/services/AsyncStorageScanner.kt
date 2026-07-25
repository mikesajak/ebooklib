package com.mikesajak.ebooklib.admin.application.services

import com.mikesajak.ebooklib.admin.domain.model.ScanStatus
import com.mikesajak.ebooklib.admin.domain.model.StorageScanStats
import com.mikesajak.ebooklib.admin.infrastructure.incoming.web.StorageScanStatsDto
import com.mikesajak.ebooklib.book.application.ports.outgoing.BookCoverMetadataRepositoryPort
import com.mikesajak.ebooklib.book.application.ports.outgoing.EbookFormatFileRepositoryPort
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileStoragePort
import com.mikesajak.ebooklib.importing.application.ports.outgoing.StagedEbookUploadRepositoryPort
import com.mikesajak.ebooklib.notification.application.NotificationService
import com.mikesajak.ebooklib.notification.domain.model.NotificationEvent
import com.mikesajak.ebooklib.notification.domain.model.NotificationType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

private val logger = KotlinLogging.logger {}

@Service
class AsyncStorageScanner(
    private val fileStoragePort: FileStoragePort,
    private val bookFormatRepository: EbookFormatFileRepositoryPort,
    private val bookCoverRepository: BookCoverMetadataRepositoryPort,
    private val stagingRepository: StagedEbookUploadRepositoryPort,
    private val notificationService: NotificationService
) {
    private val currentStats = AtomicReference(
        StorageScanStats(ScanStatus.IDLE, null, null, 0, 0, 0, 0, emptyList(), 0)
    )

    private fun StorageScanStats.toDto() = StorageScanStatsDto(
        status = status,
        startedAt = startedAt,
        finishedAt = finishedAt,
        totalFilesScanned = totalFilesScanned,
        totalScannedSize = totalScannedSize,
        orphanedFilesFound = orphanedFilesFound,
        orphanedSize = orphanedSize,
        orphanedFileKeys = orphanedFileKeys,
        progressPercent = progressPercent,
        error = error
    )

    fun getLatestStats(): StorageScanStats = currentStats.get()

    @Async
    fun startScan() {
        if (currentStats.get().status == ScanStatus.RUNNING || currentStats.get().status == ScanStatus.PURGING) {
            logger.warn { "Operation already in progress, skipping start request." }
            return
        }

        logger.info { "Starting deep storage scan..." }
        val initialStats = StorageScanStats(ScanStatus.RUNNING, Instant.now(), null, 0, 0, 0, 0, emptyList(), 0)
        currentStats.set(initialStats)
        notificationService.broadcast(NotificationEvent(NotificationType.STORAGE_SCAN_PROGRESS, initialStats.toDto()))

        try {
            // Load all referenced keys from DB (to avoid N+1 queries during scan)
            val formatKeys = bookFormatRepository.findAllKeys()
            val coverKeys = bookCoverRepository.findAllKeys()
            val stagingKeys = stagingRepository.findAllKeys()
            
            val allReferencedKeys = (formatKeys + coverKeys + stagingKeys).toSet()
            logger.info { "Found ${allReferencedKeys.size} referenced files in database." }

            val orphanedKeys = mutableListOf<String>()
            var scannedCount = 0
            var scannedSize = 0L
            var orphanedSize = 0L

            fileStoragePort.listAllFiles().forEach { entry ->
                scannedCount++
                scannedSize += entry.size
                if (!allReferencedKeys.contains(entry.key)) {
                    orphanedKeys.add(entry.key)
                    orphanedSize += entry.size
                }
                
                if (scannedCount % 50 == 0) {
                    updateProgress(scannedCount, scannedSize, orphanedKeys.size, orphanedSize, orphanedKeys.toList(), 0)
                }
            }

            val finalStats = currentStats.get().copy(
                status = ScanStatus.COMPLETED,
                finishedAt = Instant.now(),
                totalFilesScanned = scannedCount,
                totalScannedSize = scannedSize,
                orphanedFilesFound = orphanedKeys.size,
                orphanedSize = orphanedSize,
                orphanedFileKeys = orphanedKeys.toList(),
                progressPercent = 100
            )
            currentStats.set(finalStats)
            notificationService.broadcast(NotificationEvent(NotificationType.STORAGE_SCAN_PROGRESS, finalStats.toDto()))
            logger.info { "Deep storage scan completed. Found ${orphanedKeys.size} orphans (${orphanedSize} bytes). Total scanned: $scannedCount (${scannedSize} bytes)" }

        } catch (e: Exception) {
            logger.error(e) { "Error during deep storage scan" }
            val errorStats = currentStats.get().copy(
                status = ScanStatus.FAILED,
                finishedAt = Instant.now(),
                error = e.message
            )
            currentStats.set(errorStats)
            notificationService.broadcast(NotificationEvent(NotificationType.STORAGE_SCAN_PROGRESS, errorStats.toDto()))
        }
    }

    @Async
    fun purgeOrphans() {
        val stats = currentStats.get()
        if (stats.status == ScanStatus.RUNNING || stats.status == ScanStatus.PURGING) {
            logger.warn { "Operation already in progress, skipping purge request." }
            return
        }

        val keysToPurge = stats.orphanedFileKeys
        if (keysToPurge.isEmpty()) {
            logger.info { "No orphaned files to purge." }
            return
        }

        logger.info { "Starting purge of ${keysToPurge.size} orphaned files (${stats.orphanedSize} bytes)..." }
        val initialStats = stats.copy(status = ScanStatus.PURGING, progressPercent = 0)
        currentStats.set(initialStats)
        notificationService.broadcast(NotificationEvent(NotificationType.STORAGE_SCAN_PROGRESS, initialStats.toDto()))

        try {
            var purgedCount = 0
            val total = keysToPurge.size
            
            keysToPurge.forEach { key ->
                fileStoragePort.deleteFile(key)
                purgedCount++
                
                if (purgedCount % 10 == 0 || purgedCount == total) {
                    val progress = (purgedCount * 100) / total
                    updatePurgeProgress(purgedCount, total, progress)
                }
            }

            val finalStats = currentStats.get().copy(
                status = ScanStatus.COMPLETED,
                finishedAt = Instant.now(),
                orphanedFileKeys = emptyList(),
                orphanedFilesFound = 0,
                orphanedSize = 0,
                progressPercent = 100
            )
            currentStats.set(finalStats)
            notificationService.broadcast(NotificationEvent(NotificationType.STORAGE_SCAN_PROGRESS, finalStats.toDto()))
            logger.info { "Purge completed successfully. Purged $purgedCount files." }
        } catch (e: Exception) {
            logger.error(e) { "Error during orphaned files purge" }
            val errorStats = currentStats.get().copy(
                status = ScanStatus.FAILED,
                finishedAt = Instant.now(),
                error = e.message
            )
            currentStats.set(errorStats)
            notificationService.broadcast(NotificationEvent(NotificationType.STORAGE_SCAN_PROGRESS, errorStats.toDto()))
        }
    }

    private fun updateProgress(scanned: Int, scannedSize: Long, found: Int, foundSize: Long, keys: List<String>, progress: Int) {
        val stats = currentStats.get().copy(
            totalFilesScanned = scanned,
            totalScannedSize = scannedSize,
            orphanedFilesFound = found,
            orphanedSize = foundSize,
            orphanedFileKeys = keys,
            progressPercent = progress
        )
        currentStats.set(stats)
        notificationService.broadcast(NotificationEvent(NotificationType.STORAGE_SCAN_PROGRESS, stats.toDto()))
    }

    private fun updatePurgeProgress(purged: Int, total: Int, progress: Int) {
        val stats = currentStats.get()
        val currentOrphanedSize = stats.orphanedSize
        // We don't track size per file during purge for simplicity, 
        // we'll just set it to 0 at the end.
        val updatedStats = stats.copy(
            progressPercent = progress,
            orphanedFilesFound = total - purged
        )
        currentStats.set(updatedStats)
        notificationService.broadcast(NotificationEvent(NotificationType.STORAGE_SCAN_PROGRESS, updatedStats))
    }
}
