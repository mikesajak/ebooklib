package com.mikesajak.ebooklib.series.infrastructure.adapters.incoming.rest.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SeriesRequestDto(
    @field:NotBlank(message = "{series.title.notblank}")
    @field:Size(min = 1, max = 255, message = "{series.title.size}")
    val title: String,
    val description: String?
)
