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
import java.util.concurrent.atomic.AtomicInteger
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
        if (currentStats.get().status == ScanStatus.RUNNING) {
            logger.warn { "Scan already in progress, skipping start request." }
            return
        }

        logger.info { "Starting deep storage scan..." }
        currentStats.set(StorageScanStats(ScanStatus.RUNNING, Instant.now(), null, 0, 0, emptyList(), 0))

        try {
            // 1. Gather all file keys from S3
            val allPhysicalKeys = fileStoragePort.listAllFiles()
            val total = allPhysicalKeys.size
            logger.info { "Found $total physical files in storage. Starting verification..." }

            // 2. Load all referenced keys from DB (to avoid N+1 queries during scan)
            // For now, doing it simple. If we have millions of files, we need better approach.
            val formatKeys = bookFormatRepository.findAllKeys()
            val coverKeys = bookCoverRepository.findAllKeys()
            val stagingKeys = stagingRepository.findAllKeys()
            
            val allReferencedKeys = formatKeys.toSet() + coverKeys + stagingKeys
            logger.info { "Found ${allReferencedKeys.size} referenced files in database." }

            val orphanedKeys = mutableListOf<String>()
            val scannedCount = AtomicInteger(0)

            allPhysicalKeys.forEach { key ->
                if (!allReferencedKeys.contains(key)) {
                    orphanedKeys.add(key)
                }
                
                val currentCount = scannedCount.incrementAndGet()
                if (currentCount % 10 == 0 || currentCount == total) {
                    val progress = if (total > 0) (currentCount * 100) / total else 100
                    updateProgress(currentCount, orphanedKeys.size, orphanedKeys.toList(), progress)
                }
            }

            currentStats.set(currentStats.get().copy(
                status = ScanStatus.COMPLETED,
                finishedAt = Instant.now(),
                totalFilesScanned = total,
                orphanedFilesFound = orphanedKeys.size,
                orphanedFileKeys = orphanedKeys.toList(),
                progressPercent = 100
            ))
            logger.info { "Deep storage scan completed. Found ${orphanedKeys.size} orphans." }

        } catch (e: Exception) {
            logger.error(e) { "Error during deep storage scan" }
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
}
