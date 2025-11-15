package com.mikesajak.ebooklib.search.application.services

import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.common.domain.model.PaginationRequest
import com.mikesajak.ebooklib.common.domain.model.SortDirection
import com.mikesajak.ebooklib.common.domain.model.SortOrder
import com.mikesajak.ebooklib.search.application.ports.outgoing.SearchRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class SearchServiceTest {

    private lateinit var searchRepositoryPort: SearchRepositoryPort
    private lateinit var searchService: SearchService

    @BeforeEach
    fun setUp() {
        searchRepositoryPort = mockk()
        searchService = SearchService(searchRepositoryPort)
    }

    @Test
    fun `search should return paginated list of books`() {
        // Given
        val book1 = Book(BookId(UUID.randomUUID()), "Book A", emptyList(),
                         null, null, null, null,
                         null, null, emptyList())
        val book2 = Book(BookId(UUID.randomUUID()), "Book B", emptyList(),
                         null, null, null, null,
                         null, null, emptyList())
        val bookList = listOf(book1, book2)
        val query = "title==\"*A*\""
        val paginationRequest = PaginationRequest(0, 10, listOf(SortOrder("title", SortDirection.ASC)))
        val paginatedResult = PaginatedResult(bookList, 0, 10, 2L, 1)

        every { searchRepositoryPort.search(query, paginationRequest) } returns paginatedResult

        // When
        val result = searchService.search(query, paginationRequest)

        // Then
        assertEquals(paginatedResult, result)
        verify(exactly = 1) { searchRepositoryPort.search(query, paginationRequest) }
    }

    @Test
    fun `search should return empty paginated result when no books found`() {
        // Given
        val query = "title==\"*C*\""
        val paginationRequest = PaginationRequest(0, 10, listOf(SortOrder("title", SortDirection.ASC)))
        val emptyPaginatedResult = PaginatedResult(emptyList<Book>(), 0, 10, 0L, 0)

        every { searchRepositoryPort.search(query, paginationRequest) } returns emptyPaginatedResult

        // When
        val result = searchService.search(query, paginationRequest)

        // Then
        assertEquals(emptyPaginatedResult, result)
        verify(exactly = 1) { searchRepositoryPort.search(query, paginationRequest) }
    }
}
