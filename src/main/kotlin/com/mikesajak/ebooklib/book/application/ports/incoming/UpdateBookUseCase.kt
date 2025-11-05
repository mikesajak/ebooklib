package com.mikesajak.ebooklib.book.application.ports.incoming

import com.mikesajak.ebooklib.book.domain.model.Book

interface UpdateBookUseCase {
    fun updateBook(book: Book): Book
}