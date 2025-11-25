package com.mikesajak.ebooklib.book.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.author.infrastructure.adapters.outgoing.persistence.AuthorEntity
import com.mikesajak.ebooklib.series.infrastructure.adapters.outgoing.persistence.SeriesEntity
import jakarta.persistence.*
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "books")
data class BookEntity(
        @Id
        val id: UUID? = null,
        @Column(columnDefinition = "TEXT", nullable = false)
        val title: String,
        @ManyToMany
        @JoinTable(
                name = "book_authors",
                joinColumns = [JoinColumn(name = "book_id")],
                inverseJoinColumns = [JoinColumn(name = "author_id")]
        )
        val authors: Set<AuthorEntity> = emptySet(),
        @ManyToOne
        @JoinColumn(name = "series_id")
        val series: SeriesEntity?,
        val volume: Int?,
        val creationDate: LocalDate?,
        val publicationDate: LocalDate?,
        @Column(columnDefinition = "TEXT", nullable = true)
        val publisher: String?,
        @Column(columnDefinition = "TEXT", nullable = true)
        val description: String?,

        @ElementCollection(fetch = FetchType.EAGER)
        @CollectionTable(name = "book_labels", joinColumns = [JoinColumn(name = "book_id")])
        @Column(name = "label", columnDefinition = "TEXT")
        val labels: Set<String> = emptySet()
)