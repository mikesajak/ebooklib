package com.mikesajak.ebooklib.book.domain.model

import java.time.LocalDate
import java.util.UUID

data class BookId(val value: UUID)

data class Book(
    val id: BookId?,
    val title: String,
    val author: String,
    val creationDate: LocalDate?,
    val publicationDate: LocalDate?,
    val publisher: String?,
    val description: String?
)