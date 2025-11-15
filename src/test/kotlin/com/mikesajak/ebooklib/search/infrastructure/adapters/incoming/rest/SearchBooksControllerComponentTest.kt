package com.mikesajak.ebooklib.search.infrastructure.adapters.incoming.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.BookRestMapper
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.dto.BookResponseDto
import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.infrastructure.exception.GlobalExceptionHandler
import com.mikesajak.ebooklib.search.application.ports.incoming.SearchByRSQLUseCase
import com.mikesajak.ebooklib.search.infrastructure.adapters.outgoing.persistence.rsql.SearchQueryException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*

@WebMvcTest(SearchBooksController::class)
@Import(GlobalExceptionHandler::class)
class SearchBooksControllerComponentTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var searchByRSQLUseCase: SearchByRSQLUseCase

    @MockitoBean
    private lateinit var bookRestMapper: BookRestMapper

    @Test
    fun `should return paginated list of books for valid RSQL query`() {
        // Given
        val query = "title==\"Test Book\""
        val bookId1 = BookId(UUID.randomUUID())
        val book1 = Book(id = bookId1, title = "Test Book 1", authors = emptyList(),
                         creationDate = null, publicationDate = null, publisher = null, description = null,
                         series = null, volume = null, labels = emptyList())
        val paginatedResult = PaginatedResult(listOf(book1), 0, 10, 1L, 1)
        val bookResponseDto1 =
            BookResponseDto(bookId1.value, "Test Book 1", emptyList(), null, null, null, null, null, null, emptyList())

        whenever(searchByRSQLUseCase.search(any(), any())).thenReturn(paginatedResult)
        whenever(bookRestMapper.toResponse(book1)).thenReturn(bookResponseDto1)

        // When & Then
        mockMvc.perform(get("/api/books/search")
                                .param("query", query)
                                .param("page", "0")
                                .param("size", "10")
                                .param("sort", "title,asc")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(bookId1.value.toString()))
                .andExpect(jsonPath("$.content[0].title").value("Test Book 1"))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(10))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.page.totalPages").value(1))
    }

    @Test
    fun `should return empty paginated list for valid RSQL query with no results`() {
        // Given
        val query = "title==\"Non Existent Book\""
        val paginatedResult = PaginatedResult(emptyList<Book>(), 0, 10, 0L, 0)

        whenever(searchByRSQLUseCase.search(any(), any())).thenReturn(paginatedResult)

        // When & Then
        mockMvc.perform(get("/api/books/search")
                                .param("query", query)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.page.totalElements").value(0))
    }

    @Test
    fun `should return bad request for invalid RSQL query`() {
        // Given
        val invalidQuery = "title==\"Test Book" // Missing closing quote

        whenever(searchByRSQLUseCase.search(any(), any())).thenThrow(SearchQueryException("Invalid query"))

        // When & Then
        mockMvc.perform(get("/api/books/search")
                                .param("query", invalidQuery)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid query"))
    }

    @Test
    fun `should return paginated list of books for empty query`() {
        // Given
        val bookId1 = BookId(UUID.randomUUID())
        val book1 = Book(id = bookId1, title = "Any Book 1", authors = emptyList(),
                         creationDate = null, publicationDate = null, publisher = null, description = null,
                         series = null, volume = null, labels = emptyList())
        val paginatedResult = PaginatedResult(listOf(book1), 0, 10, 1L, 1)
        val bookResponseDto1 =
            BookResponseDto(bookId1.value, "Any Book 1", emptyList(), null, null, null, null, null, null, emptyList())

        whenever(searchByRSQLUseCase.search(any(), any())).thenReturn(paginatedResult)
        whenever(bookRestMapper.toResponse(book1)).thenReturn(bookResponseDto1)

        // When & Then
        mockMvc.perform(get("/api/books/search")
                                .param("query", "")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(bookId1.value.toString()))
                .andExpect(jsonPath("$.content[0].title").value("Any Book 1"))
    }
}
