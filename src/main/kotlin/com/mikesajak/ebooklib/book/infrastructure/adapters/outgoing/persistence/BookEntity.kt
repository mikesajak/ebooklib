package com.mikesajak.ebooklib.book.infrastructure.adapters.outgoing.persistence

import jakarta.persistence.*
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "books")
data class BookEntity(
    @Id
    val id: UUID? = null,
    val title: String,
    val author: String,
    val creationDate: LocalDate?,
    val publicationDate: LocalDate?,
    val publisher: String?,
    val description: String?
)