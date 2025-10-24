package com.mikesajak.ebooklib.series.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.series.domain.model.Series
import com.mikesajak.ebooklib.series.domain.model.SeriesId
import org.springframework.stereotype.Component

@Component
class SeriesEntityMapper {
    fun toEntity(series: Series): SeriesEntity =
        SeriesEntity(
            id = series.id?.value,
            title = series.title,
            description = series.description
        )

    fun toDomain(seriesEntity: SeriesEntity): Series =
        Series(
            id = seriesEntity.id?.let { SeriesId(it) },
            title = seriesEntity.title,
            description = seriesEntity.description
        )
}