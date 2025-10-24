package com.mikesajak.ebooklib.book.domain.model
import com.mikesajak.ebooklib.author.domain.model.Author

import java.time.LocalDate
import java.util.UUID

data class BookId(val value: UUID) {
    override fun toString(): String = value.toString()
}

data class Book(
    val id: BookId?,
    val title: String,
    val authors: List<Author>,
    val creationDate: LocalDate?,
    val publicationDate: LocalDate?,
    val publisher: String?,
    val description: String?
)