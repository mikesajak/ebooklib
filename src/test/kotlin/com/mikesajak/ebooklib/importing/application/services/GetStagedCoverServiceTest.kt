package com.mikesajak.ebooklib.importing.application.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileMetadata
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileStoragePort
import com.mikesajak.ebooklib.importing.application.ports.outgoing.StagedEbookUploadRepositoryPort
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUpload
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadId
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadStatus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.time.Instant
import java.util.*

class GetStagedCoverServiceTest {

    private val repository = mockk<StagedEbookUploadRepositoryPort>()
    private val fileStoragePort = mockk<FileStoragePort>()
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    private val service = GetStagedCoverService(repository, fileStoragePort, objectMapper)

    @Test
    fun `should return staged cover`() {
        // given
        val uploadId = UUID.randomUUID()
        val coverId = "staged/covers/cover123"
        val metadataJson = """{"coverStorageKey": "$coverId"}"""
        
        val stagedUpload = StagedEbookUpload(
            id = StagedEbookUploadId(uploadId),
            fileName = "test.epub",
            contentType = "application/epub+zip",
            fileSize = 1000L,
            metadataJson = metadataJson,
            status = StagedEbookUploadStatus.PARSED,
            createdAt = Instant.now(),
            expiryAt = Instant.now().plusSeconds(3600)
        )

        val coverData = "cover content".toByteArray()
        val fileMetadata = FileMetadata(coverId, "cover.jpg", "image/jpeg", coverData.size.toLong())

        every { repository.findById(StagedEbookUploadId(uploadId)) } returns stagedUpload
        every { fileStoragePort.getFileMetadata(coverId) } returns fileMetadata
        every { fileStoragePort.downloadFile(coverId) } returns ByteArrayInputStream(coverData)

        // when
        val result = service.getCover(StagedEbookUploadId(uploadId))

        // then
        assertThat(result).isNotNull
        assertThat(result?.contentType).isEqualTo("image/jpeg")
        assertThat(result?.inputStream?.readAllBytes()).isEqualTo(coverData)
    }

    @Test
    fun `should return null if upload not found`() {
        // given
        val uploadId = UUID.randomUUID()
        every { repository.findById(StagedEbookUploadId(uploadId)) } returns null

        // when
        val result = service.getCover(StagedEbookUploadId(uploadId))

        // then
        assertThat(result).isNull()
    }

    @Test
    fun `should return null if no coverStorageKey in metadata`() {
        // given
        val uploadId = UUID.randomUUID()
        val metadataJson = """{"title": "No Cover Here"}"""
        
        val stagedUpload = StagedEbookUpload(
            id = StagedEbookUploadId(uploadId),
            fileName = "test.epub",
            contentType = "application/epub+zip",
            fileSize = 1000L,
            metadataJson = metadataJson,
            status = StagedEbookUploadStatus.PARSED,
            createdAt = Instant.now(),
            expiryAt = Instant.now().plusSeconds(3600)
        )

        every { repository.findById(StagedEbookUploadId(uploadId)) } returns stagedUpload

        // when
        val result = service.getCover(StagedEbookUploadId(uploadId))

        // then
        assertThat(result).isNull()
    }
}
