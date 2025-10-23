package com.mikesajak.ebooklib.book.application.ports.outgoing

import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.domain.model.BookId

interface BookRepositoryPort {
    fun findAll(): List<Book>
    fun findById(id: BookId): Book?
    fun save(book: Book): Book
}