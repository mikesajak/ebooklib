package com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.dto

import java.time.LocalDate

data class BookRequestDto(
    val title: String,
    val authorIds: List<java.util.UUID>,
    val seriesId: java.util.UUID?,
    val volume: Int?,
    val creationDate: LocalDate?,
    val publicationDate: LocalDate?,
    val publisher: String?,
    val description: String?
)