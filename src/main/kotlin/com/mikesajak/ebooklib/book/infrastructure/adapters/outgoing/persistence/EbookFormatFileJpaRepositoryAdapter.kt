package com.mikesajak.ebooklib.book.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.book.application.ports.outgoing.EbookFormatFileRepositoryPort
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.book.domain.model.EbookFormatFile
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class EbookFormatFileJpaRepositoryAdapter(
    private val ebookFormatFileJpaRepository: EbookFormatFileJpaRepository
) : EbookFormatFileRepositoryPort {
    override fun save(ebookFormatFile: EbookFormatFile): EbookFormatFile {
        return ebookFormatFileJpaRepository.save(ebookFormatFile.toEntity()).toDomain()
    }

    override fun findByBookId(bookId: BookId): List<EbookFormatFile> {
        return ebookFormatFileJpaRepository.findByBookId(bookId.value).map { it.toDomain() }
    }

    override fun findByBookIdAndId(bookId: BookId, id: UUID): EbookFormatFile? {
        return ebookFormatFileJpaRepository.findByBookIdAndId(bookId.value, id)?.toDomain()
    }

    override fun delete(ebookFormatFile: EbookFormatFile) {
        ebookFormatFileJpaRepository.delete(ebookFormatFile.toEntity())
    }

    private fun EbookFormatFileEntity.toDomain(): EbookFormatFile {
        return EbookFormatFile(
            id = this.id,
            bookId = BookId(this.bookId),
            storageKey = this.storageKey,
            fileName = this.fileName,
            contentType = this.contentType,
            fileSize = this.fileSize,
            formatType = this.formatType
        )
    }

    private fun EbookFormatFile.toEntity(): EbookFormatFileEntity {
        return EbookFormatFileEntity(
            id = this.id,
            bookId = this.bookId.value,
            storageKey = this.storageKey,
            fileName = this.fileName,
            contentType = this.contentType,
            fileSize = this.fileSize,
            formatType = this.formatType
        )
    }
}
