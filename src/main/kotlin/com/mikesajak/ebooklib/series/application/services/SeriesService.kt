package com.mikesajak.ebooklib.series.application.services

import com.mikesajak.ebooklib.series.application.ports.incoming.GetSeriesUseCase
import com.mikesajak.ebooklib.series.application.ports.outgoing.SeriesRepositoryPort
import com.mikesajak.ebooklib.series.domain.exception.SeriesNotFoundException
import com.mikesajak.ebooklib.series.domain.model.Series
import com.mikesajak.ebooklib.series.domain.model.SeriesId
import org.springframework.stereotype.Service

@Service
class SeriesService(private val seriesRepository: SeriesRepositoryPort) : GetSeriesUseCase {

    override fun getSeries(seriesId: SeriesId): Series {
        return seriesRepository.findById(seriesId)
            ?: throw SeriesNotFoundException(seriesId)
    }

    override fun getAllSeries(): List<Series> {
        return seriesRepository.findAll()
    }
}