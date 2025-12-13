package com.mikesajak.ebooklib.series.application.services

import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.common.domain.model.PaginationRequest
import com.mikesajak.ebooklib.common.domain.model.SortDirection
import com.mikesajak.ebooklib.common.domain.model.SortOrder
import com.mikesajak.ebooklib.series.application.ports.incoming.UpdateSeriesCommand
import com.mikesajak.ebooklib.series.application.ports.outgoing.SeriesRepositoryPort
import com.mikesajak.ebooklib.series.domain.exception.SeriesNotFoundException
import com.mikesajak.ebooklib.series.domain.model.Series
import com.mikesajak.ebooklib.series.domain.model.SeriesId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import java.util.*

class SeriesServiceTest {

    private lateinit var seriesRepository: SeriesRepositoryPort
    private lateinit var seriesService: SeriesService

    @BeforeEach
    fun setUp() {
        seriesRepository = mockk()
        seriesService = SeriesService(seriesRepository)
    }

    @Test
    fun `getSeries should return series when found`() {
        // Given
        val seriesId = SeriesId(UUID.randomUUID())
        val series = Series(seriesId, "Test Series", null)
        every { seriesRepository.findById(seriesId) } returns series

        // When
        val result = seriesService.getSeries(seriesId)

        // Then
        assertEquals(series, result)
        verify(exactly = 1) { seriesRepository.findById(seriesId) }
    }

    @Test
    fun `getSeries should throw SeriesNotFoundException when not found`() {
        // Given
        val seriesId = SeriesId(UUID.randomUUID())
        every { seriesRepository.findById(seriesId) } returns null

        // When & Then
        assertThrows(SeriesNotFoundException::class.java) {
            seriesService.getSeries(seriesId)
        }
        verify(exactly = 1) { seriesRepository.findById(seriesId) }
    }

    @Test
    fun `getAllSeries should return paginated list of series`() {
        // Given
        val series1 = Series(SeriesId(UUID.randomUUID()), "Series A", null)
        val series2 = Series(SeriesId(UUID.randomUUID()), "Series B", null)
        val seriesList = listOf(series1, series2)
        val paginationRequest = PaginationRequest(0, 10, listOf(SortOrder("name", SortDirection.ASC)))
        val paginatedResult = PaginatedResult(seriesList, 0, 10, 2L, 1)

        every { seriesRepository.findAll(paginationRequest) } returns paginatedResult

        // When
        val result = seriesService.getAllSeries(paginationRequest)

        // Then
        assertEquals(paginatedResult, result)
        verify(exactly = 1) { seriesRepository.findAll(paginationRequest) }
    }

    @Test
    fun `getAllSeries should return empty paginated result when no series found`() {
        // Given
        val paginationRequest = PaginationRequest(0, 10, listOf(SortOrder("name", SortDirection.ASC)))
        val emptyPaginatedResult = PaginatedResult(emptyList<Series>(), 0, 10, 0L, 0)

        every { seriesRepository.findAll(paginationRequest) } returns emptyPaginatedResult

        // When
        val result = seriesService.getAllSeries(paginationRequest)

        // Then
        assertEquals(emptyPaginatedResult, result)
        verify(exactly = 1) { seriesRepository.findAll(paginationRequest) }
    }

    @Test
    fun `should update a series`() {
        // Given
        val seriesId = SeriesId(UUID.randomUUID())
        val existingSeries = Series(seriesId, "Old Title", "Old Description")
        val command = UpdateSeriesCommand(seriesId, "New Title", "New Description")
        val updatedSeries = Series(seriesId, "New Title", "New Description")

        every { seriesRepository.findById(seriesId) } returns existingSeries
        every { seriesRepository.save(updatedSeries) } returns updatedSeries

        // When
        val result = seriesService.updateSeries(command)

        // Then
        assertEquals(updatedSeries, result)
        verify(exactly = 1) { seriesRepository.findById(seriesId) }
        verify(exactly = 1) { seriesRepository.save(updatedSeries) }
    }

    @Test
    fun `should throw SeriesNotFoundException when updating non-existent series`() {
        // Given
        val seriesId = SeriesId(UUID.randomUUID())
        val command = UpdateSeriesCommand(seriesId, "New Title", "New Description")

        every { seriesRepository.findById(seriesId) } returns null

        // When & Then
        assertThrows(SeriesNotFoundException::class.java) {
            seriesService.updateSeries(command)
        }
        verify(exactly = 1) { seriesRepository.findById(seriesId) }
        verify(exactly = 0) { seriesRepository.save(any()) }
    }
}
