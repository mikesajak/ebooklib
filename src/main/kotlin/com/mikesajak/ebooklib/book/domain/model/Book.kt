package com.mikesajak.ebooklib.book.domain.model

import com.mikesajak.ebooklib.author.domain.model.Author
import com.mikesajak.ebooklib.series.domain.model.Series
import java.time.LocalDate
import java.util.*

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
        val description: String?,
        val series: Series?,
        val volume: Int?,
        val labels: List<String> = emptyList()
)