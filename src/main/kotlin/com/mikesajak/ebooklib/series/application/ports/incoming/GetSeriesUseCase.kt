package com.mikesajak.ebooklib.series.application.ports.incoming

import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.common.domain.model.PaginationRequest
import com.mikesajak.ebooklib.series.domain.model.Series
import com.mikesajak.ebooklib.series.domain.model.SeriesId

interface GetSeriesUseCase {
    fun getSeries(seriesId: SeriesId): Series
    fun getAllSeries(pagination: PaginationRequest): PaginatedResult<Series>
}