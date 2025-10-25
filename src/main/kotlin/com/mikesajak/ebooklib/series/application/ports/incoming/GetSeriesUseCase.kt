package com.mikesajak.ebooklib.series.application.ports.incoming
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

import com.mikesajak.ebooklib.series.domain.model.Series
import com.mikesajak.ebooklib.series.domain.model.SeriesId

interface GetSeriesUseCase {
    fun getSeries(seriesId: SeriesId): Series
    fun getAllSeries(pageable: Pageable): Page<Series>
}