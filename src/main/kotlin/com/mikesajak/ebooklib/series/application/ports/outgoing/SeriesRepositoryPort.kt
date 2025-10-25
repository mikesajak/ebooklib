package com.mikesajak.ebooklib.series.application.ports.outgoing
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

import com.mikesajak.ebooklib.series.domain.model.Series
import com.mikesajak.ebooklib.series.domain.model.SeriesId

interface SeriesRepositoryPort {
    fun findAll(pageable: Pageable): Page<Series>
    fun findById(id: SeriesId): Series?
    fun save(series: Series): Series
}