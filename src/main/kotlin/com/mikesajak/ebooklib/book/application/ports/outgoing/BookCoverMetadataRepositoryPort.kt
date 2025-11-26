package com.mikesajak.ebooklib.book.application.ports.outgoing

import com.mikesajak.ebooklib.book.domain.model.BookCoverMetadata
import com.mikesajak.ebooklib.book.domain.model.BookId

interface BookCoverMetadataRepositoryPort {
    fun findByBookId(bookId: BookId): BookCoverMetadata?
    fun save(bookCover: BookCoverMetadata): BookCoverMetadata
    fun delete(bookCover: BookCoverMetadata)
    fun existsByBookId(bookId: BookId): Boolean
}
