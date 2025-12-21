package com.mikesajak.ebooklib.search.infrastructure.adapters.incoming.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.mikesajak.ebooklib.series.domain.model.Series
import com.mikesajak.ebooklib.series.domain.model.SeriesId
import com.mikesajak.ebooklib.series.infrastructure.adapters.incoming.rest.SeriesRestMapper
import com.mikesajak.ebooklib.series.infrastructure.adapters.incoming.rest.dto.SeriesResponseDto
import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.infrastructure.exception.GlobalExceptionHandler
import com.mikesajak.ebooklib.search.application.ports.incoming.SearchSeriesUseCase
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

@WebMvcTest(SearchSeriesController::class)
@Import(GlobalExceptionHandler::class)
class SearchSeriesControllerComponentTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var searchSeriesUseCase: SearchSeriesUseCase

    @MockitoBean
    private lateinit var seriesRestMapper: SeriesRestMapper

    @Test
    fun `should return paginated list of series for valid RSQL query`() {
        // Given
        val query = "title==\"Foundation\""
        val seriesId1 = SeriesId(UUID.randomUUID())
        val series1 = Series(id = seriesId1, title = "Foundation", description = "Sci-fi")
        val paginatedResult = PaginatedResult(listOf(series1), 0, 10, 1L, 1)
        val seriesResponseDto1 = 
            SeriesResponseDto(seriesId1.value, "Foundation", "Sci-fi")

        whenever(searchSeriesUseCase.search(any(), any())).thenReturn(paginatedResult)
        whenever(seriesRestMapper.toResponse(series1)).thenReturn(seriesResponseDto1)

        // When & Then
        mockMvc.perform(get("/api/series/search")
                                .param("query", query)
                                .param("page", "0")
                                .param("size", "10")
                                .param("sort", "title,asc")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(seriesId1.value.toString()))
                .andExpect(jsonPath("$.content[0].title").value("Foundation"))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(10))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.page.totalPages").value(1))
    }

    @Test
    fun `should return empty paginated list for valid RSQL query with no results`() {
        // Given
        val query = "title==\"NonExistent\""
        val paginatedResult = PaginatedResult(emptyList<Series>(), 0, 10, 0L, 0)

        whenever(searchSeriesUseCase.search(any(), any())).thenReturn(paginatedResult)

        // When & Then
        mockMvc.perform(get("/api/series/search")
                                .param("query", query)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.page.totalElements").value(0))
    }

    @Test
    fun `should return bad request for invalid RSQL query`() {
        // Given
        val invalidQuery = "title==\"Foundation" // Missing closing quote

        whenever(searchSeriesUseCase.search(any(), any())).thenThrow(SearchQueryException("Invalid query"))

        // When & Then
        mockMvc.perform(get("/api/series/search")
                                .param("query", invalidQuery)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid query"))
    }
}
