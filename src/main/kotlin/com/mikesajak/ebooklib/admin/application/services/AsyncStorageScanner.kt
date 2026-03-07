package com.mikesajak.ebooklib.admin.application.services

import com.mikesajak.ebooklib.admin.domain.model.ScanStatus
import com.mikesajak.ebooklib.admin.domain.model.StorageScanStats
import com.mikesajak.ebooklib.book.application.ports.outgoing.BookCoverMetadataRepositoryPort
import com.mikesajak.ebooklib.book.application.ports.outgoing.EbookFormatFileRepositoryPort
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileStoragePort
import com.mikesajak.ebooklib.importing.application.ports.outgoing.StagedEbookUploadRepositoryPort
import mu.KotlinLogging
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
    private val stagingRepository: StagedEbookUploadRepositoryPort
) {
    private val currentStats = AtomicReference(
        StorageScanStats(ScanStatus.IDLE, null, null, 0, 0, emptyList(), 0)
    )

    fun getLatestStats(): StorageScanStats = currentStats.get()

    @Async
    fun startScan() {
        if (currentStats.get().status == ScanStatus.RUNNING || currentStats.get().status == ScanStatus.PURGING) {
            logger.warn { "Operation already in progress, skipping start request." }
            return
        }

        logger.info { "Starting deep storage scan..." }
        currentStats.set(StorageScanStats(ScanStatus.RUNNING, Instant.now(), null, 0, 0, emptyList(), 0))

        try {
            // Load all referenced keys from DB (to avoid N+1 queries during scan)
            val formatKeys = bookFormatRepository.findAllKeys()
            val coverKeys = bookCoverRepository.findAllKeys()
            val stagingKeys = stagingRepository.findAllKeys()
            
            val allReferencedKeys = (formatKeys + coverKeys + stagingKeys).toSet()
            logger.info { "Found ${allReferencedKeys.size} referenced files in database." }

            val orphanedKeys = mutableListOf<String>()
            var scannedCount = 0

            fileStoragePort.listAllFiles().forEach { key ->
                scannedCount++
                if (!allReferencedKeys.contains(key)) {
                    orphanedKeys.add(key)
                }
                
                if (scannedCount % 50 == 0) {
                    updateProgress(scannedCount, orphanedKeys.size, orphanedKeys.toList(), 0)
                }
            }

            currentStats.set(currentStats.get().copy(
                status = ScanStatus.COMPLETED,
                finishedAt = Instant.now(),
                totalFilesScanned = scannedCount,
                orphanedFilesFound = orphanedKeys.size,
                orphanedFileKeys = orphanedKeys.toList(),
                progressPercent = 100
            ))
            logger.info { "Deep storage scan completed. Found ${orphanedKeys.size} orphans. Total scanned: $scannedCount" }

        } catch (e: Exception) {
            logger.error(e) { "Error during deep storage scan" }
            currentStats.set(currentStats.get().copy(
                status = ScanStatus.FAILED,
                finishedAt = Instant.now(),
                error = e.message
            ))
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

        logger.info { "Starting purge of ${keysToPurge.size} orphaned files..." }
        currentStats.set(stats.copy(status = ScanStatus.PURGING, progressPercent = 0))

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

            currentStats.set(currentStats.get().copy(
                status = ScanStatus.COMPLETED,
                finishedAt = Instant.now(),
                orphanedFileKeys = emptyList(),
                orphanedFilesFound = 0,
                progressPercent = 100
            ))
            logger.info { "Purge completed successfully. Purged $purgedCount files." }
        } catch (e: Exception) {
            logger.error(e) { "Error during orphaned files purge" }
            currentStats.set(currentStats.get().copy(
                status = ScanStatus.FAILED,
                finishedAt = Instant.now(),
                error = e.message
            ))
        }
    }

    private fun updateProgress(scanned: Int, found: Int, keys: List<String>, progress: Int) {
        currentStats.set(currentStats.get().copy(
            totalFilesScanned = scanned,
            orphanedFilesFound = found,
            orphanedFileKeys = keys,
            progressPercent = progress
        ))
    }

    private fun updatePurgeProgress(purged: Int, total: Int, progress: Int) {
        currentStats.set(currentStats.get().copy(
            progressPercent = progress,
            orphanedFilesFound = total - purged
        ))
    }
}
