package com.mikesajak.ebooklib.admin.application.services

import com.mikesajak.ebooklib.admin.domain.model.ScanStatus
import com.mikesajak.ebooklib.book.application.ports.outgoing.BookCoverMetadataRepositoryPort
import com.mikesajak.ebooklib.book.application.ports.outgoing.EbookFormatFileRepositoryPort
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileEntry
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileStoragePort
import com.mikesajak.ebooklib.importing.application.ports.outgoing.StagedEbookUploadRepositoryPort
import com.mikesajak.ebooklib.notification.application.NotificationService
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AsyncStorageScannerTest {

    private val fileStoragePort = mockk<FileStoragePort>()
    private val bookFormatRepository = mockk<EbookFormatFileRepositoryPort>()
    private val bookCoverRepository = mockk<BookCoverMetadataRepositoryPort>()
    private val stagingRepository = mockk<StagedEbookUploadRepositoryPort>()
    private val notificationService = mockk<NotificationService>(relaxed = true)

    private lateinit var scanner: AsyncStorageScanner

    @BeforeEach
    fun setUp() {
        scanner = AsyncStorageScanner(
            fileStoragePort,
            bookFormatRepository,
            bookCoverRepository,
            stagingRepository,
            notificationService
        )
    }

    @Test
    fun `startScan should identify orphaned files and update stats including sizes`() {
        // Given
        val physicalFiles = sequenceOf(
            FileEntry("key1", 100),
            FileEntry("key2", 200),
            FileEntry("key3", 300),
            FileEntry("orphan1", 1000),
            FileEntry("orphan2", 2000)
        )
        val formatKeys = listOf("key1")
        val coverKeys = listOf("key2")
        val stagingKeys = listOf("key3")

        every { fileStoragePort.listAllFiles() } returns physicalFiles
        every { bookFormatRepository.findAllKeys() } returns formatKeys
        every { bookCoverRepository.findAllKeys() } returns coverKeys
        every { stagingRepository.findAllKeys() } returns stagingKeys

        // When
        scanner.startScan()

        // Then
        val stats = scanner.getLatestStats()
        assertEquals(ScanStatus.COMPLETED, stats.status)
        assertEquals(5, stats.totalFilesScanned)
        assertEquals(3600, stats.totalScannedSize)
        assertEquals(2, stats.orphanedFilesFound)
        assertEquals(3000, stats.orphanedSize)
        assertTrue(stats.orphanedFileKeys.contains("orphan1"))
        assertTrue(stats.orphanedFileKeys.contains("orphan2"))
        assertEquals(100, stats.progressPercent)

        verify { fileStoragePort.listAllFiles() }
        verify { notificationService.broadcast(any()) }
    }

    @Test
    fun `purgeOrphans should delete orphaned files and reset size stats`() {
        // Given - run a scan first to populate orphaned keys
        val physicalFiles = sequenceOf(FileEntry("key1", 100), FileEntry("orphan1", 500))
        every { fileStoragePort.listAllFiles() } returns physicalFiles
        every { bookFormatRepository.findAllKeys() } returns listOf("key1")
        every { bookCoverRepository.findAllKeys() } returns emptyList()
        every { stagingRepository.findAllKeys() } returns emptyList()
        every { fileStoragePort.deleteFile(any()) } returns Unit

        scanner.startScan()
        val statsBeforePurge = scanner.getLatestStats()
        assertEquals(500, statsBeforePurge.orphanedSize)

        // When
        scanner.purgeOrphans()

        // Then
        val statsAfterPurge = scanner.getLatestStats()
        assertEquals(ScanStatus.COMPLETED, statsAfterPurge.status)
        assertEquals(0, statsAfterPurge.orphanedFilesFound)
        assertEquals(0, statsAfterPurge.orphanedSize)
        assertTrue(statsAfterPurge.orphanedFileKeys.isEmpty())
        assertEquals(100, statsAfterPurge.progressPercent)

        verify { fileStoragePort.deleteFile("orphan1") }
    }
}
