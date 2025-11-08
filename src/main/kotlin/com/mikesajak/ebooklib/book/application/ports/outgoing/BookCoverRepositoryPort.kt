package com.mikesajak.ebooklib.book.application.ports.outgoing

import com.mikesajak.ebooklib.book.domain.model.BookCover
import com.mikesajak.ebooklib.book.domain.model.BookId

interface BookCoverRepositoryPort {
    fun findByBookId(bookId: BookId): BookCover?
    fun save(bookCover: BookCover): BookCover
    fun delete(bookCover: BookCover)
    fun existsByBookId(bookId: BookId): Boolean
}
