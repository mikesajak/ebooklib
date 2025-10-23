package com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.dto

import java.time.LocalDate

data class BookRequestDto(
    val title: String,
    val author: String,
    val creationDate: LocalDate?,
    val publicationDate: LocalDate?,
    val publisher: String?,
    val description: String?
)