package com.mikesajak.ebooklib.book.domain.model

import java.io.InputStream
import java.util.*

data class BookCoverMetadata(
        val id: UUID,
        val bookId: BookId,
        val storageKey: String,
        val fileName: String,
        val contentType: String,
        val fileSize: Long
)

data class BookCover(
        val metadata: BookCoverMetadata,
        val inputStream: InputStream
)
