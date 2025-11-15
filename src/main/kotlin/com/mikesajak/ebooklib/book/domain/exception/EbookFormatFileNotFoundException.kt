package com.mikesajak.ebooklib.book.domain.exception

import com.mikesajak.ebooklib.book.domain.model.BookId
import java.util.*

class EbookFormatFileNotFoundException(bookId: BookId, formatFileId: UUID) :
        RuntimeException("Ebook format file with ID: $formatFileId for book with ID: $bookId not found")
