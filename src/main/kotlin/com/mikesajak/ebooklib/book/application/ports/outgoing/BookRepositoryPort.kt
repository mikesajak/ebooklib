package com.mikesajak.ebooklib.book.application.ports.outgoing
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

import com.mikesajak.ebooklib.author.domain.model.AuthorId
import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.series.domain.model.SeriesId

interface BookRepositoryPort {
    fun findAll(pageable: Pageable): Page<Book>
    fun findById(id: BookId): Book?
    fun save(book: Book): Book
    fun update(book: Book): Book
    fun delete(bookId: BookId)
    fun findByAuthorId(authorId: AuthorId, pageable: Pageable): Page<Book>
    fun findBySeriesId(seriesId: SeriesId, pageable: Pageable): Page<Book>
}