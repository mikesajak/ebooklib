package com.mikesajak.ebooklib.series.infrastructure.adapters.incoming.rest.dto

import java.util.UUID

data class SeriesResponseDto(
    val id: UUID,
    val title: String,
    val description: String?
)