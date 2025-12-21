package com.mikesajak.ebooklib.search.application.services

import com.mikesajak.ebooklib.series.domain.model.Series
import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.common.domain.model.PaginationRequest
import com.mikesajak.ebooklib.search.application.ports.incoming.SearchSeriesUseCase
import com.mikesajak.ebooklib.search.application.ports.outgoing.SearchSeriesRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SearchSeriesService(
        private val searchSeriesRepositoryPort: SearchSeriesRepositoryPort
) : SearchSeriesUseCase {

    @Transactional(readOnly = true)
    override fun search(query: String, pagination: PaginationRequest): PaginatedResult<Series> {
        return searchSeriesRepositoryPort.search(query, pagination)
    }
}
