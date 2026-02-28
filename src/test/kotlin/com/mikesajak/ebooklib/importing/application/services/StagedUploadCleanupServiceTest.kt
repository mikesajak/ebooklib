package com.mikesajak.ebooklib.importing.application.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileStoragePort
import com.mikesajak.ebooklib.importing.application.ports.outgoing.StagedEbookUploadRepositoryPort
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUpload
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadId
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

class StagedUploadCleanupServiceTest {

    private val repository = mockk<StagedEbookUploadRepositoryPort>()
    private val fileStoragePort = mockk<FileStoragePort>()
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    private val service = StagedUploadCleanupService(repository, fileStoragePort, objectMapper)

    @Test
    fun `should cleanup expired uploads`() {
        // given
        val expiredId = UUID.randomUUID()
        val coverKey = "staged/covers/expired-cover"
        val expiredUpload = StagedEbookUpload(
            id = StagedEbookUploadId(expiredId),
            fileName = "expired.epub",
            contentType = "application/epub+zip",
            fileSize = 100L,
            metadataJson = """{"coverStorageKey": "$coverKey"}""",
            status = StagedEbookUploadStatus.PARSED,
            createdAt = Instant.now().minusSeconds(86400 * 2),
            expiryAt = Instant.now().minusSeconds(3600)
        )

        every { repository.findByExpiryAtBefore(any()) } returns listOf(expiredUpload)
        every { fileStoragePort.deleteFile(any()) } returns Unit
        every { repository.delete(any()) } returns Unit

        // when
        service.cleanupExpiredUploads()

        // then
        verify(exactly = 1) { fileStoragePort.deleteFile("staged/$expiredId") }
        verify(exactly = 1) { fileStoragePort.deleteFile(coverKey) }
        verify(exactly = 1) { repository.delete(StagedEbookUploadId(expiredId)) }
    }

    @Test
    fun `should continue cleanup if one upload fails`() {
        // given
        val upload1 = createExpiredUpload(UUID.randomUUID())
        val upload2 = createExpiredUpload(UUID.randomUUID())

        every { repository.findByExpiryAtBefore(any()) } returns listOf(upload1, upload2)
        every { fileStoragePort.deleteFile("staged/${upload1.id}") } throws RuntimeException("Storage error")
        every { fileStoragePort.deleteFile("staged/${upload2.id}") } returns Unit
        every { repository.delete(any()) } returns Unit

        // when
        service.cleanupExpiredUploads()

        // then
        verify(exactly = 1) { fileStoragePort.deleteFile("staged/${upload1.id}") }
        verify(exactly = 1) { fileStoragePort.deleteFile("staged/${upload2.id}") }
        verify(exactly = 1) { repository.delete(upload2.id) }
    }

    private fun createExpiredUpload(id: UUID) = StagedEbookUpload(
        id = StagedEbookUploadId(id),
        fileName = "test.epub",
        contentType = "application/epub+zip",
        fileSize = 100L,
        metadataJson = "{}",
        status = StagedEbookUploadStatus.PARSED,
        createdAt = Instant.now().minusSeconds(86400 * 2),
        expiryAt = Instant.now().minusSeconds(3600)
    )
}
