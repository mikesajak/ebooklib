package com.mikesajak.ebooklib.book.application.ports.incoming

import com.mikesajak.ebooklib.book.domain.model.BookId

interface DeleteBookUseCase {
    fun deleteBook(bookId: BookId)
}
