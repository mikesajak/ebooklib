package com.mikesajak.ebooklib.series.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.series.application.ports.outgoing.SeriesRepositoryPort
import com.mikesajak.ebooklib.series.domain.model.Series
import com.mikesajak.ebooklib.series.domain.model.SeriesId
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Repository
@Profile("dummy")
class DummySeriesRepository : SeriesRepositoryPort {

    private val storage = ConcurrentHashMap<UUID, Series>()

    override fun findAll(): List<Series> = storage.values.toList()

    override fun findById(id: SeriesId): Series? = storage[id.value]

    override fun save(series: Series): Series {
        val idToUse = series.id?.value ?: UUID.randomUUID()
        val seriesToSave = series.copy(id = SeriesId(idToUse))
        storage[idToUse] = seriesToSave
        return seriesToSave
    }
}