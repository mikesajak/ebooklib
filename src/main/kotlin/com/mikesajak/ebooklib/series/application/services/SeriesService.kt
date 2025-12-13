package com.mikesajak.ebooklib.series.application.services

import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.common.domain.model.PaginationRequest
import com.mikesajak.ebooklib.series.application.ports.incoming.AddSeriesUseCase
import com.mikesajak.ebooklib.series.application.ports.incoming.GetSeriesUseCase
import com.mikesajak.ebooklib.series.application.ports.incoming.UpdateSeriesCommand
import com.mikesajak.ebooklib.series.application.ports.incoming.UpdateSeriesUseCase
import com.mikesajak.ebooklib.series.application.ports.outgoing.SeriesRepositoryPort
import com.mikesajak.ebooklib.series.domain.exception.SeriesNotFoundException
import com.mikesajak.ebooklib.series.domain.model.Series
import com.mikesajak.ebooklib.series.domain.model.SeriesId
import org.springframework.stereotype.Service

@Service
class SeriesService(
    private val seriesRepository: SeriesRepositoryPort
) : GetSeriesUseCase, AddSeriesUseCase, UpdateSeriesUseCase {

    override fun getSeries(seriesId: SeriesId): Series =
        seriesRepository.findById(seriesId)
            ?: throw SeriesNotFoundException(seriesId)


    override fun getAllSeries(pagination: PaginationRequest): PaginatedResult<Series> =
        seriesRepository.findAll(pagination)

    override fun addSeries(series: Series): Series =
        seriesRepository.save(series)

    override fun updateSeries(command: UpdateSeriesCommand): Series {
        val series = seriesRepository.findById(command.id)
            ?: throw SeriesNotFoundException(command.id)

        val updatedSeries = series.copy(
            title = command.title,
            description = command.description
        )

        return seriesRepository.save(updatedSeries)
    }

}