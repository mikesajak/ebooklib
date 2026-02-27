package com.mikesajak.ebooklib.importing.application.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileMetadata
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileStoragePort
import com.mikesajak.ebooklib.importing.application.ports.incoming.EbookMetadataExtractorUseCase
import com.mikesajak.ebooklib.importing.application.ports.outgoing.StagedEbookUploadRepositoryPort
import com.mikesajak.ebooklib.importing.domain.model.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.time.Instant
import java.util.*

class UploadToStagingServiceTest {

    private val fileStoragePort = mockk<FileStoragePort>()
    private val metadataExtractor = mockk<EbookMetadataExtractorUseCase>()
    private val repository = mockk<StagedEbookUploadRepositoryPort>()
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    private val service = UploadToStagingService(fileStoragePort, metadataExtractor, repository, objectMapper)

    @Test
    fun `should upload and parse ebook`() {
        // given
        val fileContent = "dummy content".toByteArray()
        val fileName = "test.epub"
        val contentType = "application/epub+zip"
        val fileId = UUID.randomUUID().toString()
        
        val extractedMetadata = ExtractedEbookMetadata(
            title = "Test Title",
            authors = listOf("Author 1"),
            creationDate = null,
            publicationDate = null,
            publisher = "Test Publisher",
            description = "Test Description",
            coverImage = ExtractedCoverImage("cover.jpg", "image/jpeg", "fake image data".toByteArray())
        )

        every { metadataExtractor.extract(any(), any(), any()) } returns extractedMetadata
        every { fileStoragePort.uploadFile(any(), any(), any(), any()) } returnsMany listOf(
            FileMetadata(fileId, fileName, contentType, fileContent.size.toLong()),
            FileMetadata("cover-id", "cover.jpg", "image/jpeg", 100L)
        )
        every { repository.save(any()) } answers { firstArg() }

        // when
        val result = service.upload(ByteArrayInputStream(fileContent), fileName, contentType)

        // then
        assertThat(result.id.value).isEqualTo(UUID.fromString(fileId))
        assertThat(result.fileName).isEqualTo(fileName)
        assertThat(result.status).isEqualTo(StagedEbookUploadStatus.PARSED)
        
        val metadataMap = objectMapper.readValue(result.metadataJson, Map::class.java)
        assertThat(metadataMap["title"]).isEqualTo("Test Title")
        assertThat(metadataMap["coverStorageKey"]).isEqualTo("cover-id")

        verify(exactly = 1) { fileStoragePort.uploadFile(any(), fileName, contentType, "staged") }
        verify(exactly = 1) { fileStoragePort.uploadFile(any(), "cover.jpg", "image/jpeg", "staged/covers") }
        verify(exactly = 1) { repository.save(any()) }
    }
}
