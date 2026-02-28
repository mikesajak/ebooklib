package com.mikesajak.ebooklib.importing.infrastructure.adapters.incoming.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.mikesajak.ebooklib.author.infrastructure.adapters.incoming.rest.AuthorRestMapper
import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.BookRestMapper
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.BookView
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.dto.BookResponseDto
import com.mikesajak.ebooklib.importing.application.ports.incoming.FinalizeImportUseCase
import com.mikesajak.ebooklib.importing.application.ports.incoming.GetStagedCoverUseCase
import com.mikesajak.ebooklib.importing.application.ports.incoming.StagedCover
import com.mikesajak.ebooklib.importing.application.ports.incoming.UploadToStagingUseCase
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUpload
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadId
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadStatus
import com.mikesajak.ebooklib.importing.infrastructure.adapters.incoming.rest.dto.FinalizeImportRequestDto
import com.mikesajak.ebooklib.infrastructure.exception.GlobalExceptionHandler
import com.mikesajak.ebooklib.infrastructure.security.SecurityConfig
import com.mikesajak.ebooklib.series.infrastructure.adapters.incoming.rest.SeriesRestMapper
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.io.ByteArrayInputStream
import java.time.Instant
import java.util.*

@WebMvcTest(ImportController::class)
@Import(
    GlobalExceptionHandler::class,
    SecurityConfig::class,
    ImportRestMapper::class,
    BookRestMapper::class,
    AuthorRestMapper::class,
    SeriesRestMapper::class
)
@ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = ["app.security.enabled=false"])
class ImportControllerComponentTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var uploadToStagingUseCase: UploadToStagingUseCase

    @MockitoBean
    private lateinit var getStagedCoverUseCase: GetStagedCoverUseCase

    @MockitoBean
    private lateinit var finalizeImportUseCase: FinalizeImportUseCase

    @MockitoBean
    private lateinit var bookRestMapper: BookRestMapper

    @Test
    fun `should upload ebook to staging`() {
        // Given
        val fileContent = "test ebook content".toByteArray()
        val fileName = "ebook.epub"
        val contentType = "application/epub+zip"
        val multipartFile = MockMultipartFile("file", fileName, contentType, fileContent)
        
        val stagedUpload = StagedEbookUpload(
            id = StagedEbookUploadId(UUID.randomUUID()),
            fileName = fileName,
            contentType = contentType,
            fileSize = fileContent.size.toLong(),
            metadataJson = """{"title": "Test Book", "authors": ["Author 1"]}""",
            status = StagedEbookUploadStatus.PARSED,
            createdAt = Instant.now(),
            expiryAt = Instant.now().plusSeconds(3600)
        )

        whenever(uploadToStagingUseCase.upload(any(), any(), any(), anyOrNull())).thenReturn(stagedUpload)

        // When & Then
        mockMvc.perform(multipart("/api/import/upload")
            .file(multipartFile))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(stagedUpload.id.toString()))
            .andExpect(jsonPath("$.fileName").value(stagedUpload.fileName))
            .andExpect(jsonPath("$.metadata.title").value("Test Book"))
            .andExpect(jsonPath("$.status").value("PARSED"))
    }

    @Test
    fun `should upload ebook and return match candidates`() {
        // Given
        val fileContent = "test ebook content".toByteArray()
        val fileName = "ebook.epub"
        val contentType = "application/epub+zip"
        val multipartFile = MockMultipartFile("file", fileName, contentType, fileContent)
        val matchingBookId = UUID.randomUUID()

        val stagedUpload = StagedEbookUpload(
            id = StagedEbookUploadId(UUID.randomUUID()),
            fileName = fileName,
            contentType = contentType,
            fileSize = fileContent.size.toLong(),
            metadataJson = """
                {
                  "title": "Hobbit",
                  "validation": {
                    "candidates": [
                      {
                        "bookId": "$matchingBookId",
                        "title": "The Hobbit",
                        "authors": ["J.R.R. Tolkien"],
                        "titleMatch": false,
                        "authorMatch": false,
                        "score": 50
                      }
                    ]
                  }
                }
            """.trimIndent(),
            status = StagedEbookUploadStatus.PARSED,
            createdAt = Instant.now(),
            expiryAt = Instant.now().plusSeconds(3600)
        )

        whenever(uploadToStagingUseCase.upload(any(), any(), any(), anyOrNull())).thenReturn(stagedUpload)

        // When & Then
        mockMvc.perform(multipart("/api/import/upload")
            .file(multipartFile))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.validation.candidates").isArray)
            .andExpect(jsonPath("$.validation.candidates[0].bookId").value(matchingBookId.toString()))
            .andExpect(jsonPath("$.validation.candidates[0].title").value("The Hobbit"))
            .andExpect(jsonPath("$.validation.candidates[0].score").value(50))
    }

    @Test
    fun `should upload ebook to staging with currentBookId`() {
        // Given
        val currentBookId = UUID.randomUUID()
        val fileContent = "test ebook content".toByteArray()
        val fileName = "ebook.epub"
        val contentType = "application/epub+zip"
        val multipartFile = MockMultipartFile("file", fileName, contentType, fileContent)
        
        val stagedUpload = StagedEbookUpload(
            id = StagedEbookUploadId(UUID.randomUUID()),
            fileName = fileName,
            contentType = contentType,
            fileSize = fileContent.size.toLong(),
            metadataJson = """{"title": "Test Book"}""",
            status = StagedEbookUploadStatus.PARSED,
            createdAt = Instant.now(),
            expiryAt = Instant.now().plusSeconds(3600)
        )

        whenever(uploadToStagingUseCase.upload(any(), any(), any(), anyOrNull())).thenReturn(stagedUpload)

        // When & Then
        mockMvc.perform(multipart("/api/import/upload")
            .file(multipartFile)
            .param("currentBookId", currentBookId.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(stagedUpload.id.toString()))
    }

    @Test
    fun `should serve staged cover`() {
        // Given
        val uploadId = UUID.randomUUID()
        val coverData = "fake image data".toByteArray()
        val stagedCover = StagedCover(ByteArrayInputStream(coverData), "image/jpeg")

        whenever(getStagedCoverUseCase.getCover(StagedEbookUploadId(uploadId))).thenReturn(stagedCover)

        // When & Then
        mockMvc.perform(get("/api/import/staged/$uploadId/cover"))
            .andExpect(status().isOk)
            .andExpect(content().contentType("image/jpeg"))
            .andExpect(content().bytes(coverData))
    }

    @Test
    fun `should return 404 when staged cover not found`() {
        // Given
        val uploadId = UUID.randomUUID()
        whenever(getStagedCoverUseCase.getCover(StagedEbookUploadId(uploadId))).thenReturn(null)

        // When & Then
        mockMvc.perform(get("/api/import/staged/$uploadId/cover"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should finalize import`() {
        // Given
        val uploadId = UUID.randomUUID()
        val bookId = UUID.randomUUID()
        val request = FinalizeImportRequestDto(
            uploadId = uploadId,
            title = "Finalized Book",
            authorIds = listOf(UUID.randomUUID())
        )

        val finalizedBook = mockk<Book>()
        val responseDto = BookResponseDto(
            id = bookId,
            title = "Finalized Book",
            authors = emptyList(),
            series = null,
            volume = null,
            creationDate = null,
            publicationDate = null,
            publisher = null,
            description = null,
            labels = emptyList()
        )

        whenever(finalizeImportUseCase.finalize(any())).thenReturn(finalizedBook)
        whenever(bookRestMapper.toResponse(finalizedBook, BookView.FULL)).thenReturn(responseDto)

        // When & Then
        mockMvc.perform(post("/api/import/finalize")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(bookId.toString()))
            .andExpect(jsonPath("$.title").value("Finalized Book"))
    }
}
