package com.mikesajak.ebooklib.book.domain.exception

import com.mikesajak.ebooklib.book.domain.model.BookId

class BookCoverFileMissingException(bookId: BookId) :
    RuntimeException("Book cover file for book with ID ${bookId.value} not found in storage, but metadata exists.")
