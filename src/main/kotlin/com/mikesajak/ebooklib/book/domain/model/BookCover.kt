package com.mikesajak.ebooklib.book.domain.model

import java.util.UUID

data class BookCover(
    val id: UUID,
    val bookId: BookId,
    val storageKey: String,
    val fileName: String,
    val contentType: String,
    val fileSize: Long
)
