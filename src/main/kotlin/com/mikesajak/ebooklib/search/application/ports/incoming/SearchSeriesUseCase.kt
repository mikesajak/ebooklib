package com.mikesajak.ebooklib.search.application.ports.incoming

import com.mikesajak.ebooklib.series.domain.model.Series
import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.common.domain.model.PaginationRequest

interface SearchSeriesUseCase {
    fun search(query: String, pagination: PaginationRequest): PaginatedResult<Series>
}
