package com.mikesajak.ebooklib.importing.application.services

import com.mikesajak.ebooklib.file.application.ports.outgoing.FileMetadata
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileStoragePort
import com.mikesajak.ebooklib.importing.application.ports.outgoing.StagedEbookUploadRepositoryPort
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUpload
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadId
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadStatus
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
    private val repository = mockk<StagedEbookUploadRepositoryPort>()
    private val stagedUploadProcessor = mockk<StagedUploadProcessor>()

    private val service = UploadToStagingService(
        fileStoragePort, repository, stagedUploadProcessor
    )

    @Test
    fun `should upload and process synchronously`() {
        // given
        val fileContent = "dummy content".toByteArray()
        val fileName = "test.epub"
        val contentType = "application/epub+zip"
        val fileId = "staged/" + UUID.randomUUID().toString()
        val uploadId = UUID.fromString(fileId.substringAfterLast('/'))
        
        val stagedUpload = StagedEbookUpload(
            StagedEbookUploadId(uploadId), fileName, contentType, fileContent.size.toLong(), null, StagedEbookUploadStatus.PROCESSING, Instant.now(), Instant.now()
        )
        val processedUpload = stagedUpload.copy(status = StagedEbookUploadStatus.PARSED)

        every { fileStoragePort.uploadFile(any(), any(), any(), any()) } returns FileMetadata(fileId, fileName, contentType, fileContent.size.toLong())
        every { repository.save(any()) } returns stagedUpload
        every { stagedUploadProcessor.process(any(), any(), any(), any(), any()) } returns processedUpload

        // when
        val result = service.upload(ByteArrayInputStream(fileContent), fileName, contentType, null)

        // then
        assertThat(result).isEqualTo(processedUpload)
        verify { fileStoragePort.uploadFile(any(), fileName, contentType, "staged") }
        verify { repository.save(match { it.status == StagedEbookUploadStatus.PROCESSING }) }
        verify { stagedUploadProcessor.process(StagedEbookUploadId(uploadId), fileContent, fileName, contentType, null) }
    }
    
    @Test
    fun `should upload and process asynchronously`() {
        // given
        val fileContent = "dummy content".toByteArray()
        val fileName = "test.epub"
        val contentType = "application/epub+zip"
        val fileId = "staged/" + UUID.randomUUID().toString()
        val uploadId = UUID.fromString(fileId.substringAfterLast('/'))
        
        val stagedUpload = StagedEbookUpload(
            StagedEbookUploadId(uploadId), fileName, contentType, fileContent.size.toLong(), null, StagedEbookUploadStatus.PROCESSING, Instant.now(), Instant.now()
        )

        every { fileStoragePort.uploadFile(any(), any(), any(), any()) } returns FileMetadata(fileId, fileName, contentType, fileContent.size.toLong())
        every { repository.save(any()) } returns stagedUpload
        every { stagedUploadProcessor.processAsync(any(), any(), any(), any(), any()) } returns Unit

        // when
        val result = service.uploadAsync(ByteArrayInputStream(fileContent), fileName, contentType, null)

        // then
        assertThat(result).isEqualTo(stagedUpload)
        verify { fileStoragePort.uploadFile(any(), fileName, contentType, "staged") }
        verify { repository.save(match { it.status == StagedEbookUploadStatus.PROCESSING }) }
        verify { stagedUploadProcessor.processAsync(StagedEbookUploadId(uploadId), fileContent, fileName, contentType, null) }
    }
}
