package com.mikesajak.ebooklib.search.infrastructure.adapters.incoming.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.mikesajak.ebooklib.author.application.projection.AuthorProjection
import com.mikesajak.ebooklib.author.infrastructure.adapters.incoming.rest.AuthorRestMapper
import com.mikesajak.ebooklib.author.infrastructure.adapters.incoming.rest.dto.AuthorResponseDto
import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.infrastructure.exception.GlobalExceptionHandler
import com.mikesajak.ebooklib.search.application.ports.incoming.SearchAuthorsUseCase
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

@WebMvcTest(SearchAuthorsController::class)
@Import(GlobalExceptionHandler::class)
class SearchAuthorsControllerComponentTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var searchAuthorsUseCase: SearchAuthorsUseCase

    @MockitoBean
    private lateinit var authorRestMapper: AuthorRestMapper

    @Test
    fun `should return paginated list of authors for valid RSQL query`() {
        // Given
        val query = "lastName==\"Doe\""
        val authorId1 = UUID.randomUUID()
        val authorProjection1 = AuthorProjection(id = authorId1, firstName = "John", lastName = "Doe", bio = "Author bio", birthDate = null, deathDate = null, bookCount = 5)
        val paginatedResult = PaginatedResult(listOf(authorProjection1), 0, 10, 1L, 1)
        val authorResponseDto1 =
            AuthorResponseDto(authorId1, "John", "Doe", "Author bio", null, null, 5)

        whenever(searchAuthorsUseCase.search(any(), any())).thenReturn(paginatedResult)
        whenever(authorRestMapper.toResponse(authorProjection1)).thenReturn(authorResponseDto1)

        // When & Then
        mockMvc.perform(get("/api/authors/search")
                                .param("query", query)
                                .param("page", "0")
                                .param("size", "10")
                                .param("sort", "lastName,asc")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(authorId1.toString()))
                .andExpect(jsonPath("$.content[0].firstName").value("John"))
                .andExpect(jsonPath("$.content[0].lastName").value("Doe"))
                .andExpect(jsonPath("$.content[0].bookCount").value(5))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(10))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.page.totalPages").value(1))
    }

    @Test
    fun `should return empty paginated list for valid RSQL query with no results`() {
        // Given
        val query = "lastName==\"NonExistent\""
        val paginatedResult = PaginatedResult(emptyList<AuthorProjection>(), 0, 10, 0L, 0)

        whenever(searchAuthorsUseCase.search(any(), any())).thenReturn(paginatedResult)

        // When & Then
        mockMvc.perform(get("/api/authors/search")
                                .param("query", query)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.page.totalElements").value(0))
    }

    @Test
    fun `should return bad request for invalid RSQL query`() {
        // Given
        val invalidQuery = "lastName==\"Doe" // Missing closing quote

        whenever(searchAuthorsUseCase.search(any(), any())).thenThrow(SearchQueryException("Invalid query"))

        // When & Then
        mockMvc.perform(get("/api/authors/search")
                                .param("query", invalidQuery)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid query"))
    }
}
