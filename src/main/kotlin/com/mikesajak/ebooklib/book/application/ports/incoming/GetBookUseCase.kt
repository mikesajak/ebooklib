package com.mikesajak.ebooklib.book.application.ports.incoming

import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.common.domain.model.PaginationRequest

interface GetBookUseCase {
    fun getBook(bookId: BookId): Book
    fun getAllBooks(pagination: PaginationRequest): PaginatedResult<Book>
    fun getNewestBooks(pagination: PaginationRequest): PaginatedResult<Book>
}