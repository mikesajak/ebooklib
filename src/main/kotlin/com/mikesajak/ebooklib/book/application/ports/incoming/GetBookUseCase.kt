package com.mikesajak.ebooklib.book.application.ports.incoming
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.domain.model.BookId

interface GetBookUseCase {
    fun getBook(bookId: BookId): Book
    fun getAllBooks(pageable: Pageable): Page<Book>
}