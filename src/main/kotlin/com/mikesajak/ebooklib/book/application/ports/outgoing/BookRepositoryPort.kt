package com.mikesajak.ebooklib.book.application.ports.outgoing

import com.mikesajak.ebooklib.author.domain.model.AuthorId
import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.common.domain.model.PaginationRequest
import com.mikesajak.ebooklib.series.domain.model.SeriesId

interface BookRepositoryPort {
    fun findAll(pagination: PaginationRequest): PaginatedResult<Book>
    fun findById(id: BookId): Book?
    fun save(book: Book): Book
    fun update(book: Book): Book
    fun delete(bookId: BookId)
    fun findByAuthorId(authorId: AuthorId, pagination: PaginationRequest): PaginatedResult<Book>
    fun findBySeriesId(seriesId: SeriesId, pagination: PaginationRequest): PaginatedResult<Book>
}