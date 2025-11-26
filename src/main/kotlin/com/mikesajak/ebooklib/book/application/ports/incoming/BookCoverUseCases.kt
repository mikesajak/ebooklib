package com.mikesajak.ebooklib.book.application.ports.incoming

import com.mikesajak.ebooklib.book.domain.model.BookCover
import com.mikesajak.ebooklib.book.domain.model.BookCoverMetadata
import com.mikesajak.ebooklib.book.domain.model.BookId
import java.io.InputStream

interface UploadBookCoverUseCase {
    fun uploadCover(bookId: BookId, fileContent: InputStream, originalFileName: String, contentType: String): BookCoverMetadata
}

interface GetBookCoverUseCase {
    fun hasCover(bookId: BookId): Boolean
    fun getCover(bookId: BookId): BookCover
    fun getCoverIfExists(bookId: BookId): BookCover?
}

interface DeleteBookCoverUseCase {
    fun deleteCover(bookId: BookId)
}
