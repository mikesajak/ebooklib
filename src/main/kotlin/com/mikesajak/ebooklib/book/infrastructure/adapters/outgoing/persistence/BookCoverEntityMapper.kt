package com.mikesajak.ebooklib.book.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.book.domain.model.BookCoverMetadata
import com.mikesajak.ebooklib.book.domain.model.BookId
import org.springframework.stereotype.Component

@Component
class BookCoverEntityMapper {
    fun toEntity(bookCover: BookCoverMetadata): BookCoverEntity =
        BookCoverEntity(
                id = bookCover.id,
                bookId = bookCover.bookId.value,
                storageKey = bookCover.storageKey,
                fileName = bookCover.fileName,
                contentType = bookCover.contentType,
                fileSize = bookCover.fileSize
        )

    fun toDomain(bookCoverEntity: BookCoverEntity): BookCoverMetadata =
        BookCoverMetadata(
                id = bookCoverEntity.id,
                bookId = BookId(bookCoverEntity.bookId),
                storageKey = bookCoverEntity.storageKey,
                fileName = bookCoverEntity.fileName,
                contentType = bookCoverEntity.contentType,
                fileSize = bookCoverEntity.fileSize
        )
}
