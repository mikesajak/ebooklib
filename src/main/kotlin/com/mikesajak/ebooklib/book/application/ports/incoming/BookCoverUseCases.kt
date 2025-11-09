package com.mikesajak.ebooklib.book.application.ports.incoming

import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileMetadata
import org.springframework.core.io.Resource
import java.io.InputStream
import java.util.*

interface UploadBookCoverUseCase {
    fun uploadCover(bookId: BookId, fileContent: InputStream, originalFileName: String, contentType: String): FileMetadata
}

interface GetBookCoverUseCase {
    fun getCover(bookId: BookId): Pair<InputStream, FileMetadata>
}

interface DeleteBookCoverUseCase {
    fun deleteCover(bookId: BookId)
}

interface HasBookCoverUseCase {
    fun hasCover(bookId: BookId): Boolean
}
