package com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.mikesajak.ebooklib.book.application.ports.incoming.AddEbookFormatUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.DeleteEbookFormatUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.DownloadEbookFormatUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.ListEbookFormatsUseCase
import com.mikesajak.ebooklib.book.domain.exception.BookNotFoundException
import com.mikesajak.ebooklib.book.domain.exception.EbookFormatFileNotFoundException
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.book.domain.model.EbookFormatFile
import com.mikesajak.ebooklib.infrastructure.exception.GlobalExceptionHandler
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.io.ByteArrayInputStream
import java.util.*

@WebMvcTest(BookFormatController::class)
@Import(GlobalExceptionHandler::class)
class BookFormatControllerComponentTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var addEbookFormatUseCase: AddEbookFormatUseCase

    @MockitoBean
    private lateinit var listEbookFormatsUseCase: ListEbookFormatsUseCase

    @MockitoBean
    private lateinit var downloadEbookFormatUseCase: DownloadEbookFormatUseCase

    @MockitoBean
    private lateinit var deleteEbookFormatUseCase: DeleteEbookFormatUseCase

    @Test
    fun `should add ebook format`() {
        // Given
        val bookId = BookId(UUID.randomUUID())
        val fileContent = "test ebook content".toByteArray()
        val fileName = "ebook.epub"
        val contentType = "application/epub+zip"
        val formatType = "EPUB"
        val multipartFile = MockMultipartFile("file", fileName, contentType, fileContent)
        val ebookFormatFile = EbookFormatFile(
                UUID.randomUUID(),
                bookId,
                fileName,
                contentType,
                fileContent.size.toLong(),
                formatType,
                "storage-key-1"
        )

        whenever(addEbookFormatUseCase.addFormatFile(any(), any(), any(), any(), any())).thenReturn(ebookFormatFile)

        // When & Then
        mockMvc.perform(multipart("/api/books/{bookId}/formats", bookId.value)
                                .file(multipartFile)
                                .param("formatType", formatType))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(ebookFormatFile.id.toString()))
                .andExpect(jsonPath("$.fileName").value(ebookFormatFile.fileName))
                .andExpect(jsonPath("$.contentType").value(ebookFormatFile.contentType))
                .andExpect(jsonPath("$.size").value(ebookFormatFile.fileSize))
                .andExpect(jsonPath("$.formatType").value(ebookFormatFile.formatType))
    }

    @Test
    fun `should return 404 when adding format for non-existent book`() {
        // Given
        val bookId = BookId(UUID.randomUUID())
        val fileContent = "test ebook content".toByteArray()
        val fileName = "ebook.epub"
        val contentType = "application/epub+zip"
        val formatType = "EPUB"
        val multipartFile = MockMultipartFile("file", fileName, contentType, fileContent)

        whenever(addEbookFormatUseCase.addFormatFile(any(),
                                                     any(),
                                                     any(),
                                                     any(),
                                                     any())).thenThrow(BookNotFoundException(bookId))

        // When & Then
        mockMvc.perform(multipart("/api/books/{bookId}/formats", bookId.value)
                                .file(multipartFile)
                                .param("formatType", formatType))
                .andExpect(status().isNotFound)
    }

    @Test
    fun `should list ebook formats`() {
        // Given
        val bookId = BookId(UUID.randomUUID())
        val ebookFormatFile1 = EbookFormatFile(
                UUID.randomUUID(),
                bookId,
                "ebook1.epub",
                "application/epub+zip",
                100L,
                "EPUB",
                "storage-key-2"
        )
        val ebookFormatFile2 = EbookFormatFile(
                UUID.randomUUID(),
                bookId,
                "ebook2.pdf",
                "application/pdf",
                200L,
                "PDF",
                "storage-key-3"
        )
        val formatFiles = listOf(ebookFormatFile1, ebookFormatFile2)

        whenever(listEbookFormatsUseCase.listFormatFiles(bookId)).thenReturn(formatFiles)

        // When & Then
        mockMvc.perform(get("/api/books/{bookId}/formats", bookId.value))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(ebookFormatFile1.id.toString()))
                .andExpect(jsonPath("$[1].id").value(ebookFormatFile2.id.toString()))
    }

    @Test
    fun `should return 404 when listing formats for non-existent book`() {
        // Given
        val bookId = BookId(UUID.randomUUID())
        whenever(listEbookFormatsUseCase.listFormatFiles(bookId)).thenThrow(BookNotFoundException(bookId))

        // When & Then
        mockMvc.perform(get("/api/books/{bookId}/formats", bookId.value))
                .andExpect(status().isNotFound)
    }

    @Test
    fun `should download ebook format`() {
        // Given
        val bookId = BookId(UUID.randomUUID())
        val formatFileId = UUID.randomUUID()
        val fileContent = "test ebook content".toByteArray()
        val fileName = "ebook.epub"
        val contentType = "application/epub+zip"
        val ebookFormatFile = EbookFormatFile(
                formatFileId,
                bookId,
                fileName,
                contentType,
                fileContent.size.toLong(),
                "EPUB",
                "storage-key-4"
        )

        whenever(downloadEbookFormatUseCase.downloadFormatFile(bookId, formatFileId))
                .thenReturn(Pair(ByteArrayInputStream(fileContent), ebookFormatFile))

        // When & Then
        mockMvc.perform(get("/api/books/{bookId}/formats/{formatFileId}/download", bookId.value, formatFileId))
                .andExpect(status().isOk)
                .andExpect(content().contentType(contentType))
                .andExpect(content().bytes(fileContent))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"$fileName\""))
                .andExpect(header().longValue("Content-Length", fileContent.size.toLong()))
    }

    @Test
    fun `should return 404 when downloading format for non-existent book`() {
        // Given
        val bookId = BookId(UUID.randomUUID())
        val formatFileId = UUID.randomUUID()
        whenever(downloadEbookFormatUseCase.downloadFormatFile(bookId, formatFileId)).thenThrow(BookNotFoundException(
                bookId))

        // When & Then
        mockMvc.perform(get("/api/books/{bookId}/formats/{formatFileId}/download", bookId.value, formatFileId))
                .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 404 when downloading non-existent ebook format`() {
        // Given
        val bookId = BookId(UUID.randomUUID())
        val formatFileId = UUID.randomUUID()
        whenever(downloadEbookFormatUseCase.downloadFormatFile(bookId, formatFileId)).thenThrow(
                EbookFormatFileNotFoundException(bookId, formatFileId))

        // When & Then
        mockMvc.perform(get("/api/books/{bookId}/formats/{formatFileId}/download", bookId.value, formatFileId))
                .andExpect(status().isNotFound)
    }

    @Test
    fun `should delete ebook format`() {
        // Given
        val bookId = BookId(UUID.randomUUID())
        val formatFileId = UUID.randomUUID()
        doNothing().whenever(deleteEbookFormatUseCase).deleteFormatFile(bookId, formatFileId)

        // When & Then
        mockMvc.perform(delete("/api/books/{bookId}/formats/{formatFileId}", bookId.value, formatFileId))
                .andExpect(status().isNoContent)
    }

    @Test
    fun `should return 404 when deleting format for non-existent book`() {
        // Given
        val bookId = BookId(UUID.randomUUID())
        val formatFileId = UUID.randomUUID()
        whenever(deleteEbookFormatUseCase.deleteFormatFile(bookId,
                                                           formatFileId)).thenThrow(BookNotFoundException(bookId))

        // When & Then
        mockMvc.perform(delete("/api/books/{bookId}/formats/{formatFileId}", bookId.value, formatFileId))
                .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 404 when deleting non-existent ebook format`() {
        // Given
        val bookId = BookId(UUID.randomUUID())
        val formatFileId = UUID.randomUUID()
        whenever(deleteEbookFormatUseCase.deleteFormatFile(bookId, formatFileId)).thenThrow(
                EbookFormatFileNotFoundException(bookId, formatFileId))

        // When & Then
        mockMvc.perform(delete("/api/books/{bookId}/formats/{formatFileId}", bookId.value, formatFileId))
                .andExpect(status().isNotFound)
    }
}
