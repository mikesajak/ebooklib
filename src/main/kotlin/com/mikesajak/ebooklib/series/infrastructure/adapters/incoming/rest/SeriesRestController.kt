package com.mikesajak.ebooklib.series.infrastructure.adapters.incoming.rest

import com.mikesajak.ebooklib.series.application.ports.incoming.GetSeriesUseCase
import com.mikesajak.ebooklib.series.domain.model.SeriesId
import com.mikesajak.ebooklib.series.infrastructure.adapters.incoming.rest.dto.SeriesResponseDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/series")
class SeriesRestController(
    private val getSeriesUseCase: GetSeriesUseCase,
    private val seriesRestMapper: SeriesRestMapper
) {

    @GetMapping
    fun getAllSeries(): List<SeriesResponseDto> =
        getSeriesUseCase.getAllSeries()
            .map { series -> seriesRestMapper.toResponse(series) }

    @GetMapping("/{id}")
    fun getSeriesById(@PathVariable id: UUID): SeriesResponseDto =
        seriesRestMapper.toResponse(getSeriesUseCase.getSeries(SeriesId(id)))
}