package com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.mikesajak.ebooklib.book.application.ports.incoming.DeleteBookCoverUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.GetBookCoverUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.HasBookCoverUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.UploadBookCoverUseCase
import com.mikesajak.ebooklib.book.domain.exception.BookCoverFileMissingException
import com.mikesajak.ebooklib.book.domain.exception.BookNotFoundException
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileMetadata
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.io.ByteArrayInputStream
import java.util.*

@WebMvcTest(BookCoverController::class)
class BookCoverControllerComponentTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var uploadBookCoverUseCase: UploadBookCoverUseCase

    @MockitoBean
    private lateinit var getBookCoverUseCase: GetBookCoverUseCase

    @MockitoBean
    private lateinit var deleteBookCoverUseCase: DeleteBookCoverUseCase

    @MockitoBean
    private lateinit var hasBookCoverUseCase: HasBookCoverUseCase

    @Test
    fun `should upload book cover`() {
        // Given
        val bookId = BookId(UUID.randomUUID())
        val fileContent = "test image content".toByteArray()
        val fileName = "cover.jpg"
        val contentType = MediaType.IMAGE_JPEG_VALUE
        val multipartFile = MockMultipartFile("file", fileName, contentType, fileContent)
        val fileMetadata = FileMetadata("cover-id", fileName, contentType, fileContent.size.toLong())

        whenever(uploadBookCoverUseCase.uploadCover(any(), any(), any(), any())).thenReturn(fileMetadata)

        // When & Then
        mockMvc.perform(multipart("/api/books/{bookId}/cover", bookId.value)
                                .file(multipartFile))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(fileMetadata.id))
                .andExpect(jsonPath("$.fileName").value(fileMetadata.fileName))
                .andExpect(jsonPath("$.contentType").value(fileMetadata.contentType))
                .andExpect(jsonPath("$.size").value(fileMetadata.size))
    }

    @Test
    fun `should return 404 when uploading cover for non-existent book`() {
        // Given
        val bookId = BookId(UUID.randomUUID())
        val fileContent = "test image content".toByteArray()
        val fileName = "cover.jpg"
        val contentType = MediaType.IMAGE_JPEG_VALUE
        val multipartFile = MockMultipartFile("file", fileName, contentType, fileContent)

        whenever(uploadBookCoverUseCase.uploadCover(any(),
                                                    any(),
                                                    any(),
                                                    any())).thenThrow(BookNotFoundException(bookId))

        // When & Then
        mockMvc.perform(multipart("/api/books/{bookId}/cover", bookId.value)
                                .file(multipartFile))
                .andExpect(status().isNotFound)
    }

    @Test
    fun `should get book cover`() {
        // Given
        val bookId = BookId(UUID.randomUUID())
        val fileContent = "test image content".toByteArray()
        val fileName = "cover.jpg"
        val contentType = MediaType.IMAGE_JPEG_VALUE
        val fileMetadata = FileMetadata("cover-id", fileName, contentType, fileContent.size.toLong())

        whenever(getBookCoverUseCase.getCover(bookId)).thenReturn(Pair(ByteArrayInputStream(fileContent), fileMetadata))

        // When & Then
        mockMvc.perform(get("/api/books/{bookId}/cover", bookId.value))
                .andExpect(status().isOk)
                .andExpect(content().contentType(contentType))
                .andExpect(content().bytes(fileContent))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"$fileName\""))
                .andExpect(header().longValue("Content-Length", fileContent.size.toLong()))
    }

    @Test
    fun `should return 404 when getting cover for non-existent book`() {
        // Given
        val bookId = BookId(UUID.randomUUID())
        whenever(getBookCoverUseCase.getCover(bookId)).thenThrow(BookNotFoundException(bookId))

        // When & Then
        mockMvc.perform(get("/api/books/{bookId}/cover", bookId.value))
                .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 404 when getting non-existent book cover`() {
        // Given
        val bookId = BookId(UUID.randomUUID())
        whenever(getBookCoverUseCase.getCover(bookId)).thenThrow(BookCoverFileMissingException(bookId))

        // When & Then
        mockMvc.perform(get("/api/books/{bookId}/cover", bookId.value))
                .andExpect(status().isNotFound)
    }

    @Test
    fun `should delete book cover`() {
        // Given
        val bookId = BookId(UUID.randomUUID())
        doNothing().whenever(deleteBookCoverUseCase).deleteCover(bookId)

        // When & Then
        mockMvc.perform(delete("/api/books/{bookId}/cover", bookId.value))
                .andExpect(status().isNoContent)
    }

    @Test
    fun `should return 404 when deleting cover for non-existent book`() {
        // Given
        val bookId = BookId(UUID.randomUUID())
        doNothing().whenever(deleteBookCoverUseCase)
                .deleteCover(bookId) // This mock setup is incorrect for throwing an exception.
        // Correct way to mock for throwing exception:
        whenever(deleteBookCoverUseCase.deleteCover(bookId)).thenThrow(BookNotFoundException(bookId))

        // When & Then
        mockMvc.perform(delete("/api/books/{bookId}/cover", bookId.value))
                .andExpect(status().isNotFound)
    }

    @Test
    fun `should return true if book cover exists`() {
        // Given
        val bookId = BookId(UUID.randomUUID())
        whenever(hasBookCoverUseCase.hasCover(bookId)).thenReturn(true)

        // When & Then
        mockMvc.perform(get("/api/books/{bookId}/cover/exists", bookId.value))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.exists").value(true))
    }

    @Test
    fun `should return false if book cover does not exist`() {
        // Given
        val bookId = BookId(UUID.randomUUID())
        whenever(hasBookCoverUseCase.hasCover(bookId)).thenReturn(false)

        // When & Then
        mockMvc.perform(get("/api/books/{bookId}/cover/exists", bookId.value))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.exists").value(false))
    }

    @Test
    fun `should return 404 when checking cover existence for non-existent book`() {
        // Given
        val bookId = BookId(UUID.randomUUID())
        whenever(hasBookCoverUseCase.hasCover(bookId)).thenThrow(BookNotFoundException(bookId))

        // When & Then
        mockMvc.perform(get("/api/books/{bookId}/cover/exists", bookId.value))
                .andExpect(status().isNotFound)
    }
}
