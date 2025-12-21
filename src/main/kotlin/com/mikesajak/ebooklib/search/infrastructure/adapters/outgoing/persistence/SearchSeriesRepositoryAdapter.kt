package com.mikesajak.ebooklib.search.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.series.domain.model.Series
import com.mikesajak.ebooklib.series.infrastructure.adapters.outgoing.persistence.SeriesEntity
import com.mikesajak.ebooklib.series.infrastructure.adapters.outgoing.persistence.SeriesEntityMapper
import com.mikesajak.ebooklib.series.infrastructure.adapters.outgoing.persistence.SeriesJpaRepository
import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.common.domain.model.PaginationRequest
import com.mikesajak.ebooklib.infrastructure.web.toDomainPage
import com.mikesajak.ebooklib.infrastructure.web.toSpringPageable
import com.mikesajak.ebooklib.search.application.ports.outgoing.SearchSeriesRepositoryPort
import com.mikesajak.ebooklib.search.infrastructure.adapters.outgoing.persistence.rsql.SeriesSearchFieldMapper
import com.mikesajak.ebooklib.search.infrastructure.adapters.outgoing.persistence.rsql.RSQLSpecParser
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class SearchSeriesRepositoryAdapter(
        private val rsqlParser: RSQLSpecParser,
        private val seriesJpaRepository: SeriesJpaRepository,
        private val mapper: SeriesEntityMapper,
        private val searchFieldMapper: SeriesSearchFieldMapper
) : SearchSeriesRepositoryPort {
    override fun search(query: String, pagination: PaginationRequest): PaginatedResult<Series> {
        return searchEntity(query, pagination.toSpringPageable())
                .toDomainPage { seriesEntity -> mapper.toDomain(seriesEntity!!) }
    }

    private fun searchEntity(query: String, pageable: Pageable): Page<SeriesEntity?> {
        val querySpec = rsqlParser.parseOrNull<SeriesEntity>(query, searchFieldMapper)

        return if (querySpec == null) seriesJpaRepository.findAll(pageable)
        else seriesJpaRepository.findAll(querySpec, pageable)
    }
}
