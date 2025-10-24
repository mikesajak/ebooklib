package com.mikesajak.ebooklib.book.application.ports.outgoing

import com.mikesajak.ebooklib.author.domain.model.AuthorId
import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.series.domain.model.SeriesId

interface BookRepositoryPort {
    fun findAll(): List<Book>
    fun findById(id: BookId): Book?
    fun save(book: Book): Book
    fun findByAuthorId(authorId: AuthorId): List<Book>
    fun findBySeriesId(seriesId: SeriesId): List<Book>
}