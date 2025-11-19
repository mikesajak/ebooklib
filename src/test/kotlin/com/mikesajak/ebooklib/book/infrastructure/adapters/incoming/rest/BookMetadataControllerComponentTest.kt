package com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.mikesajak.ebooklib.book.application.ports.incoming.AddBookUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.DeleteBookUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.GetBookUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.UpdateBookUseCase
import com.mikesajak.ebooklib.book.domain.exception.BookNotFoundException
import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.dto.BookRequestDto
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.dto.BookResponseDto
import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.*

@WebMvcTest(BookMetadataController::class)
class BookMetadataControllerComponentTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var getBookUseCase: GetBookUseCase

    @MockitoBean
    private lateinit var addBookUseCase: AddBookUseCase

    @MockitoBean
    private lateinit var updateBookUseCase: UpdateBookUseCase

    @MockitoBean
    private lateinit var deleteBookUseCase: DeleteBookUseCase

    @MockitoBean
    private lateinit var bookRestMapper: BookRestMapper

    @Test
    fun `should return a page of books`() {
        // Given
        val bookId1 = BookId(UUID.randomUUID())
        val bookId2 = BookId(UUID.randomUUID())
        val book1 = createBook(bookId1, "Book 1")
        val book2 = createBook(bookId2, "Book 2")
        val bookResponseDto1 = createBookResponseDto(bookId1.value, "Book 1").copy(description = null)
        val bookResponseDto2 = createBookResponseDto(bookId2.value, "Book 2").copy(description = null)

        whenever(getBookUseCase.getAllBooks(any())).thenReturn(PaginatedResult(listOf(book1, book2), 0, 2, 2L, 1))
        whenever(bookRestMapper.toResponse(book1, BookView.COMPACT)).thenReturn(bookResponseDto1)
        whenever(bookRestMapper.toResponse(book2, BookView.COMPACT)).thenReturn(bookResponseDto2)

        // When & Then
        mockMvc.perform(get("/api/books")
                                .param("page", "0")
                                .param("size", "10")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content[0].id").value(bookId1.value.toString()))
                .andExpect(jsonPath("$.content[0].title").value("Book 1"))
                .andExpect(jsonPath("$.content[0].description").value(nullValue()))
                .andExpect(jsonPath("$.content[1].id").value(bookId2.value.toString()))
                .andExpect(jsonPath("$.content[1].title").value("Book 2"))
                .andExpect(jsonPath("$.content[1].description").value(nullValue()))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.page.totalPages").value(1))
                .andExpect(jsonPath("$.page.last").value(true))
    }

    @Test
    fun `should return a page of books with full view`() {
        // Given
        val bookId1 = BookId(UUID.randomUUID())
        val bookId2 = BookId(UUID.randomUUID())
        val book1 = createBook(bookId1, "Book 1")
        val book2 = createBook(bookId2, "Book 2")
        val bookResponseDto1 = createBookResponseDto(bookId1.value, "Book 1")
        val bookResponseDto2 = createBookResponseDto(bookId2.value, "Book 2")

        whenever(getBookUseCase.getAllBooks(any())).thenReturn(PaginatedResult(listOf(book1, book2), 0, 2, 2L, 1))
        whenever(bookRestMapper.toResponse(book1, BookView.FULL)).thenReturn(bookResponseDto1)
        whenever(bookRestMapper.toResponse(book2, BookView.FULL)).thenReturn(bookResponseDto2)

        // When & Then
        mockMvc.perform(get("/api/books")
                                .param("page", "0")
                                .param("size", "10")
                                .param("view", "FULL")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content[0].id").value(bookId1.value.toString()))
                .andExpect(jsonPath("$.content[0].title").value("Book 1"))
                .andExpect(jsonPath("$.content[0].description").value("A book description"))
                .andExpect(jsonPath("$.content[1].id").value(bookId2.value.toString()))
                .andExpect(jsonPath("$.content[1].title").value("Book 2"))
                .andExpect(jsonPath("$.content[1].description").value("A book description"))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.page.totalPages").value(1))
                .andExpect(jsonPath("$.page.last").value(true))
    }

    @Test
    fun `should return a book by id`() {
        // Given
        val bookId = BookId(UUID.randomUUID())
        val book = createBook(bookId, "Book 1")
        val bookResponseDto = createBookResponseDto(bookId.value, "Book 1")

        whenever(getBookUseCase.getBook(bookId)).thenReturn(book)
        whenever(bookRestMapper.toResponse(book, BookView.FULL)).thenReturn(bookResponseDto)

        // When & Then
        mockMvc.perform(get("/api/books/{id}", bookId.value)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(bookId.value.toString()))
                .andExpect(jsonPath("$.title").value("Book 1"))
    }

    @Test
    fun `should return 404 when book not found`() {
        // Given
        val bookId = BookId(UUID.randomUUID())
        whenever(getBookUseCase.getBook(bookId)).thenThrow(BookNotFoundException(bookId))

        // When & Then
        mockMvc.perform(get("/api/books/{id}", bookId.value)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound)
    }

    @Test
    fun `should save a book`() {
        // Given
        val bookId = BookId(UUID.randomUUID())
        val bookRequestDto = createBookRequestDto("New Book")
        val book = createBook(null, "New Book")
        val savedBook = createBook(bookId, "New Book")
        val bookResponseDto = createBookResponseDto(bookId.value, "New Book")

        whenever(bookRestMapper.toDomain(bookRequestDto)).thenReturn(book)
        whenever(addBookUseCase.addBook(book)).thenReturn(savedBook)
        whenever(bookRestMapper.toResponse(savedBook, BookView.FULL)).thenReturn(bookResponseDto)

        // When & Then
        mockMvc.perform(post("/api/books")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(bookRequestDto)))
                .andExpect(status().isCreated)
                .andExpect(header().string("Location", "/api/books/${bookId.value}"))
                .andExpect(jsonPath("$.id").value(bookId.value.toString()))
                .andExpect(jsonPath("$.title").value("New Book"))
    }

    @Test
    fun `should update a book`() {
        // Given
        val bookId = BookId(UUID.randomUUID())
        val bookRequestDto = createBookRequestDto("Updated Book")
        val book = createBook(bookId, "Updated Book")
        val updatedBook = createBook(bookId, "Updated Book")
        val bookResponseDto = createBookResponseDto(bookId.value, "Updated Book")

        whenever(bookRestMapper.toDomain(bookRequestDto)).thenReturn(book.copy(id = null))
        whenever(updateBookUseCase.updateBook(book)).thenReturn(updatedBook)
        whenever(bookRestMapper.toResponse(updatedBook, BookView.FULL)).thenReturn(bookResponseDto)

        // When & Then
        mockMvc.perform(put("/api/books/{id}", bookId.value)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(bookRequestDto)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(bookId.value.toString()))
                .andExpect(jsonPath("$.title").value("Updated Book"))
    }

    @Test
    fun `should delete a book`() {
        // Given
        val bookId = BookId(UUID.randomUUID())
        doNothing().whenever(deleteBookUseCase).deleteBook(bookId)

        // When & Then
        mockMvc.perform(delete("/api/books/{id}", bookId.value))
                .andExpect(status().isNoContent)
    }

    private fun createBook(id: BookId?, title: String): Book {
        return Book(
                id = id,
                title = title,
                authors = emptyList(),
                creationDate = null,
                publicationDate = null,
                publisher = null,
                description = "A book description",
                series = null,
                volume = null,
                labels = emptyList()
        )
    }

    private fun createBookResponseDto(id: UUID, title: String): BookResponseDto {
        return BookResponseDto(
                id = id,
                title = title,
                authors = emptyList(),
                creationDate = null,
                publicationDate = null,
                publisher = null,
                description = "A book description",
                series = null,
                volume = null,
                labels = emptyList())
    }

    private fun createBookRequestDto(title: String): BookRequestDto {
        return BookRequestDto(
                title = title,
                authorIds = emptyList(),
                creationDate = null,
                publicationDate = null,
                publisher = null,
                description = "A book description",
                seriesId = null,
                volume = null,
                labels = emptyList())
    }
}
