package com.mikesajak.ebooklib.series.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.common.domain.model.PaginationRequest
import com.mikesajak.ebooklib.infrastructure.web.toDomainPage
import com.mikesajak.ebooklib.infrastructure.web.toSpringPageable
import com.mikesajak.ebooklib.series.application.ports.outgoing.SeriesRepositoryPort
import com.mikesajak.ebooklib.series.domain.model.Series
import com.mikesajak.ebooklib.series.domain.model.SeriesId
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Repository
import java.util.*

@Repository
@Primary
class DBSeriesRepository(
        private val seriesJpaRepository: SeriesJpaRepository,
        private val mapper: SeriesEntityMapper
) : SeriesRepositoryPort {

    override fun findAll(pagination: PaginationRequest): PaginatedResult<Series> =
        seriesJpaRepository.findAll(pagination.toSpringPageable())
                .toDomainPage { seriesEntity -> mapper.toDomain(seriesEntity) }

    override fun findById(id: SeriesId): Series? =
        seriesJpaRepository.findById(id.value).map { seriesEntity -> mapper.toDomain(seriesEntity) }.orElse(null)

    override fun save(series: Series): Series {
        val entity = mapper.toEntity(series)
        val entityToSave = if (entity.id == null) entity.copy(id = UUID.randomUUID()) else entity
        val savedEntity = seriesJpaRepository.save(entityToSave)
        return mapper.toDomain(savedEntity)
    }
}