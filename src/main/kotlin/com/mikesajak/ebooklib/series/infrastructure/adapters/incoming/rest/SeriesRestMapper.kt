package com.mikesajak.ebooklib.series.infrastructure.adapters.incoming.rest

import com.mikesajak.ebooklib.series.domain.model.Series
import com.mikesajak.ebooklib.series.infrastructure.adapters.incoming.rest.dto.SeriesResponseDto
import org.springframework.stereotype.Component

@Component
class SeriesRestMapper {
    fun toResponse(series: Series): SeriesResponseDto =
        SeriesResponseDto(series.id!!.value, series.title, series.description)
}