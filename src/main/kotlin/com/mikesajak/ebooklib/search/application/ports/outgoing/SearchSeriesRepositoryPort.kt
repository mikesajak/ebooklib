package com.mikesajak.ebooklib.search.application.ports.outgoing

import com.mikesajak.ebooklib.series.domain.model.Series
import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.common.domain.model.PaginationRequest

interface SearchSeriesRepositoryPort {
    fun search(query: String, pagination: PaginationRequest): PaginatedResult<Series>
}
