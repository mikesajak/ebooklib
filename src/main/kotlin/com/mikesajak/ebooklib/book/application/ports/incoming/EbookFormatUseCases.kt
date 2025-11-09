package com.mikesajak.ebooklib.book.application.ports.incoming

import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.book.domain.model.EbookFormatFile
import java.io.InputStream
import java.util.*

interface AddEbookFormatUseCase {
    fun addFormatFile(
        bookId: BookId,
        fileContent: InputStream,
        originalFileName: String,
        contentType: String,
        formatType: String
    ): EbookFormatFile
}

interface ListEbookFormatsUseCase {
    fun listFormatFiles(bookId: BookId): List<EbookFormatFile>
}

interface DownloadEbookFormatUseCase {
    fun downloadFormatFile(bookId: BookId, formatFileId: UUID): Pair<InputStream, EbookFormatFile>
}

interface DeleteEbookFormatUseCase {
    fun deleteFormatFile(bookId: BookId, formatFileId: UUID)
}
