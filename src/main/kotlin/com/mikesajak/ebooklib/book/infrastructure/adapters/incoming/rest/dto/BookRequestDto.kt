package com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.dto

import java.time.LocalDate
import java.util.*

data class BookRequestDto(
        val title: String,
        val authorIds: List<UUID>,
        val seriesId: UUID?,
        val volume: Int?,
        val creationDate: LocalDate?,
        val publicationDate: LocalDate?,
        val publisher: String?,
        val description: String?,
        val labels: List<String>?
)