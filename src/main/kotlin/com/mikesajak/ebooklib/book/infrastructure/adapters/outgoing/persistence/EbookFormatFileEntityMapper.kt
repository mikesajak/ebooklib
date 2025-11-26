package com.mikesajak.ebooklib.book.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.book.domain.model.EbookFormatFile
import org.springframework.stereotype.Component

@Component
class EbookFormatFileEntityMapper {
    fun toEntity(ebookFormatFile: EbookFormatFile): EbookFormatFileEntity =
        EbookFormatFileEntity(
            id = ebookFormatFile.id,
            bookId = ebookFormatFile.bookId.value,
            fileName = ebookFormatFile.fileName,
            contentType = ebookFormatFile.contentType,
            fileSize = ebookFormatFile.fileSize,
            formatType = ebookFormatFile.formatType,
            storageKey = ebookFormatFile.storageKey
        )

    fun toDomain(ebookFormatFileEntity: EbookFormatFileEntity): EbookFormatFile =
        EbookFormatFile(
            id = ebookFormatFileEntity.id,
            bookId = BookId(ebookFormatFileEntity.bookId),
            fileName = ebookFormatFileEntity.fileName,
            contentType = ebookFormatFileEntity.contentType,
            fileSize = ebookFormatFileEntity.fileSize,
            formatType = ebookFormatFileEntity.formatType,
            storageKey = ebookFormatFileEntity.storageKey
        )
}
