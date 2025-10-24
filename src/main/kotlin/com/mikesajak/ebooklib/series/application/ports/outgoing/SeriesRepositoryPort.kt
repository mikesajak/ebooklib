package com.mikesajak.ebooklib.series.application.ports.outgoing

import com.mikesajak.ebooklib.series.domain.model.Series
import com.mikesajak.ebooklib.series.domain.model.SeriesId

interface SeriesRepositoryPort {
    fun findAll(): List<Series>
    fun findById(id: SeriesId): Series?
    fun save(series: Series): Series
}