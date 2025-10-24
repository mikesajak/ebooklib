package com.mikesajak.ebooklib.series.domain.model

import java.util.*

data class SeriesId(val value: UUID) {
    override fun toString() = value.toString()
}

data class Series(
    val id: SeriesId?,
    val title: String,
    val description: String?
)