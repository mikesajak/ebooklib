package com.mikesajak.ebooklib.book.application.ports.outgoing

import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.book.domain.model.EbookFormatFile
import java.util.UUID

interface EbookFormatFileRepositoryPort {
    fun save(ebookFormatFile: EbookFormatFile): EbookFormatFile
    fun findByBookId(bookId: BookId): List<EbookFormatFile>
    fun findByBookIdAndId(bookId: BookId, id: UUID): EbookFormatFile?
    fun delete(ebookFormatFile: EbookFormatFile)
}
