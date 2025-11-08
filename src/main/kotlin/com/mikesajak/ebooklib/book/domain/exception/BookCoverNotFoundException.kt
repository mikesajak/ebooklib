package com.mikesajak.ebooklib.book.domain.exception

import com.mikesajak.ebooklib.book.domain.model.BookId
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
class BookCoverNotFoundException(bookId: BookId) :
    RuntimeException("Book cover for book with ID: $bookId not found")
