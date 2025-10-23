package com.mikesajak.ebooklib.book.application.ports.incoming

import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.domain.model.BookId

interface GetBookUseCase {
    fun getBook(bookId: BookId): Book
    fun getAllBooks(): List<Book>
}