package com.mikesajak.ebooklib.admin.application.services

import com.mikesajak.ebooklib.admin.domain.model.ScanStatus
import com.mikesajak.ebooklib.book.application.ports.outgoing.BookCoverMetadataRepositoryPort
import com.mikesajak.ebooklib.book.application.ports.outgoing.EbookFormatFileRepositoryPort
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileStoragePort
import com.mikesajak.ebooklib.importing.application.ports.outgoing.StagedEbookUploadRepositoryPort
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AsyncStorageScannerTest {

    private val fileStoragePort = mockk<FileStoragePort>()
    private val bookFormatRepository = mockk<EbookFormatFileRepositoryPort>()
    private val bookCoverRepository = mockk<BookCoverMetadataRepositoryPort>()
    private val stagingRepository = mockk<StagedEbookUploadRepositoryPort>()

    private lateinit var scanner: AsyncStorageScanner

    @BeforeEach
    fun setUp() {
        scanner = AsyncStorageScanner(
            fileStoragePort,
            bookFormatRepository,
            bookCoverRepository,
            stagingRepository
        )
    }

    @Test
    fun `startScan should identify orphaned files and update stats`() {
        // Given
        val physicalFiles = sequenceOf("key1", "key2", "key3", "orphan1", "orphan2")
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
        assertEquals(2, stats.orphanedFilesFound)
        assertTrue(stats.orphanedFileKeys.contains("orphan1"))
        assertTrue(stats.orphanedFileKeys.contains("orphan2"))
        assertEquals(100, stats.progressPercent)

        verify { fileStoragePort.listAllFiles() }
        verify { bookFormatRepository.findAllKeys() }
        verify { bookCoverRepository.findAllKeys() }
        verify { stagingRepository.findAllKeys() }
    }

    @Test
    fun `purgeOrphans should delete orphaned files and update stats`() {
        // Given - run a scan first to populate orphaned keys
        val physicalFiles = sequenceOf("key1", "orphan1", "orphan2")
        every { fileStoragePort.listAllFiles() } returns physicalFiles
        every { bookFormatRepository.findAllKeys() } returns listOf("key1")
        every { bookCoverRepository.findAllKeys() } returns emptyList()
        every { stagingRepository.findAllKeys() } returns emptyList()
        every { fileStoragePort.deleteFile(any()) } returns Unit

        scanner.startScan()
        val statsBeforePurge = scanner.getLatestStats()
        assertEquals(2, statsBeforePurge.orphanedFilesFound)

        // When
        scanner.purgeOrphans()

        // Then
        val statsAfterPurge = scanner.getLatestStats()
        assertEquals(ScanStatus.COMPLETED, statsAfterPurge.status)
        assertEquals(0, statsAfterPurge.orphanedFilesFound)
        assertTrue(statsAfterPurge.orphanedFileKeys.isEmpty())
        assertEquals(100, statsAfterPurge.progressPercent)

        verify { fileStoragePort.deleteFile("orphan1") }
        verify { fileStoragePort.deleteFile("orphan2") }
    }
    
    @Test
    fun `purgeOrphans should do nothing if no orphans found`() {
        // Given
        every { fileStoragePort.listAllFiles() } returns sequenceOf("key1")
        every { bookFormatRepository.findAllKeys() } returns listOf("key1")
        every { bookCoverRepository.findAllKeys() } returns emptyList()
        every { stagingRepository.findAllKeys() } returns emptyList()

        scanner.startScan()
        
        // When
        scanner.purgeOrphans()
        
        // Then
        verify(exactly = 0) { fileStoragePort.deleteFile(any()) }
    }
}
