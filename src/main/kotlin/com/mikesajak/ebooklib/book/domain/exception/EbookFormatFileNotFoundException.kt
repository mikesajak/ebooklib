package com.mikesajak.ebooklib.book.domain.exception

import com.mikesajak.ebooklib.book.domain.model.BookId
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.util.UUID

@ResponseStatus(HttpStatus.NOT_FOUND)
class EbookFormatFileNotFoundException(bookId: BookId, formatFileId: UUID) :
    RuntimeException("Ebook format file with ID: $formatFileId for book with ID: $bookId not found")
