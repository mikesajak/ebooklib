package com.mikesajak.ebooklib.book.infrastructure.adapters.outgoing.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "book_cover")
data class BookCoverEntity(
    @Id
    val id: UUID,

    @Column(name = "book_id", nullable = false)
    val bookId: UUID,

    @Column(name = "storage_key", nullable = false)
    val storageKey: String,

    @Column(name = "file_name", nullable = false)
    val fileName: String,

    @Column(name = "content_type", nullable = false)
    val contentType: String,

    @Column(name = "file_size", nullable = false)
    val fileSize: Long
)
