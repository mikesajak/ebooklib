package com.mikesajak.ebooklib.book.infrastructure.adapters.outgoing.persistence
import com.mikesajak.ebooklib.author.infrastructure.adapters.outgoing.persistence.AuthorEntity

import jakarta.persistence.*
import java.util.*

import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "books")
data class BookEntity(
    @Id
    val id: UUID? = null,
    val title: String,
    @ManyToMany
    @JoinTable(
        name = "book_authors",
        joinColumns = [JoinColumn(name = "book_id")],
        inverseJoinColumns = [JoinColumn(name = "author_id")]
    )
    val authors: Set<AuthorEntity> = emptySet(),
    val creationDate: LocalDate?,
    val publicationDate: LocalDate?,
    val publisher: String?,
    val description: String?
)