package com.mikesajak.ebooklib.series.infrastructure.adapters.incoming.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.mikesajak.ebooklib.book.application.ports.incoming.GetBooksBySeriesUseCase
import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.BookRestMapper
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.dto.BookResponseDto
import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.infrastructure.exception.GlobalExceptionHandler
import com.mikesajak.ebooklib.series.application.ports.incoming.GetSeriesUseCase
import com.mikesajak.ebooklib.series.domain.exception.SeriesNotFoundException
import com.mikesajak.ebooklib.series.domain.model.Series
import com.mikesajak.ebooklib.series.domain.model.SeriesId
import com.mikesajak.ebooklib.series.infrastructure.adapters.incoming.rest.dto.SeriesResponseDto
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*

@WebMvcTest(SeriesRestController::class)
@Import(GlobalExceptionHandler::class)
class SeriesRestControllerComponentTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Suppress("unused")
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var getSeriesUseCase: GetSeriesUseCase

    @MockitoBean
    private lateinit var getBooksBySeriesUseCase: GetBooksBySeriesUseCase

    @MockitoBean
    private lateinit var bookRestMapper: BookRestMapper

    @MockitoBean
    private lateinit var seriesRestMapper: SeriesRestMapper

    @Test
    fun `should return all series`() {
        // Given
        val series1 = Series(SeriesId(UUID.randomUUID()), "Series A", null)
        val series2 = Series(SeriesId(UUID.randomUUID()), "Series B", null)
        val seriesList = listOf(series1, series2)
        val paginatedResult = PaginatedResult(seriesList, 0, 10, 2L, 1)
        val seriesResponseDto1 = SeriesResponseDto(series1.id!!.value, series1.title, series1.description)
        val seriesResponseDto2 = SeriesResponseDto(series2.id!!.value, series2.title, series2.description)

        whenever(getSeriesUseCase.getAllSeries(any())).thenReturn(paginatedResult)
        whenever(seriesRestMapper.toResponse(series1)).thenReturn(seriesResponseDto1)
        whenever(seriesRestMapper.toResponse(series2)).thenReturn(seriesResponseDto2)

        // When & Then
        mockMvc.perform(get("/api/series"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(series1.id!!.value.toString()))
                .andExpect(jsonPath("$.content[1].id").value(series2.id!!.value.toString()))
    }

    @Test
    fun `should return series by id`() {
        // Given
        val seriesId = SeriesId(UUID.randomUUID())
        val series = Series(seriesId, "Test Series", null)
        val seriesResponseDto = SeriesResponseDto(series.id!!.value, series.title, series.description)

        whenever(getSeriesUseCase.getSeries(seriesId)).thenReturn(series)
        whenever(seriesRestMapper.toResponse(series)).thenReturn(seriesResponseDto)

        // When & Then
        mockMvc.perform(get("/api/series/{id}", seriesId.value))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(seriesId.value.toString()))
                .andExpect(jsonPath("$.title").value("Test Series"))
    }

    @Test
    fun `should return 404 when series not found`() {
        // Given
        val seriesId = SeriesId(UUID.randomUUID())
        whenever(getSeriesUseCase.getSeries(seriesId)).thenThrow(SeriesNotFoundException(seriesId))

        // When & Then
        mockMvc.perform(get("/api/series/{id}", seriesId.value))
                .andExpect(status().isNotFound)
    }

    @Test
    fun `should return books of series`() {
        // Given
        val seriesId = SeriesId(UUID.randomUUID())
        val book1 =
            Book(BookId(UUID.randomUUID()), "Book 1", emptyList(), null, null, null, null, null, null, emptyList())
        val book2 =
            Book(BookId(UUID.randomUUID()), "Book 2", emptyList(), null, null, null, null, null, null, emptyList())
        val bookList = listOf(book1, book2)
        val paginatedResult = PaginatedResult(bookList, 0, 10, 2L, 1)
        val bookResponseDto1 =
            BookResponseDto(book1.id!!.value, book1.title, emptyList(), null, null, null, null, null, null, emptyList())
        val bookResponseDto2 =
            BookResponseDto(book2.id!!.value, book2.title, emptyList(), null, null, null, null, null, null, emptyList())

        whenever(getBooksBySeriesUseCase.getBooksOfSeries(any(), any())).thenReturn(paginatedResult)
        whenever(bookRestMapper.toResponse(book1)).thenReturn(bookResponseDto1)
        whenever(bookRestMapper.toResponse(book2)).thenReturn(bookResponseDto2)

        // When & Then
        mockMvc.perform(get("/api/series/{id}/books", seriesId.value))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(book1.id!!.value.toString()))
                .andExpect(jsonPath("$.content[1].id").value(book2.id!!.value.toString()))
    }

    @Test
    fun `should return 404 when getting books of non-existent series`() {
        // Given
        val seriesId = SeriesId(UUID.randomUUID())
        whenever(getBooksBySeriesUseCase.getBooksOfSeries(any(), any())).thenThrow(SeriesNotFoundException(seriesId))

        // When & Then
        mockMvc.perform(get("/api/series/{id}/books", seriesId.value))
                .andExpect(status().isNotFound)
    }
}
