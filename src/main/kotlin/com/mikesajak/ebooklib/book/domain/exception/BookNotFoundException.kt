package com.mikesajak.ebooklib.book.domain.exception

import com.mikesajak.ebooklib.book.domain.model.BookId

class BookNotFoundException(val bookId: BookId) : RuntimeException() {
}