package com.mikesajak.ebooklib.book.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.book.application.ports.outgoing.BookCoverMetadataRepositoryPort
import com.mikesajak.ebooklib.book.domain.model.BookCoverMetadata
import com.mikesajak.ebooklib.book.domain.model.BookId
import org.springframework.stereotype.Component

@Component
class BookCoverMetadataRepositoryAdapter(
        private val bookCoverJpaRepository: BookCoverJpaRepository
) : BookCoverMetadataRepositoryPort {
    override fun findByBookId(bookId: BookId): BookCoverMetadata? {
        return bookCoverJpaRepository.findByBookId(bookId.value)?.toDomain()
    }

    override fun save(bookCover: BookCoverMetadata): BookCoverMetadata {
        return bookCoverJpaRepository.save(bookCover.toEntity()).toDomain()
    }

    override fun delete(bookCover: BookCoverMetadata) {
        bookCoverJpaRepository.delete(bookCover.toEntity())
    }

    override fun existsByBookId(bookId: BookId): Boolean {
        return bookCoverJpaRepository.existsByBookId(bookId.value)
    }

    private fun BookCoverEntity.toDomain(): BookCoverMetadata {
        return BookCoverMetadata(
                id = this.id,
                bookId = BookId(this.bookId),
                storageKey = this.storageKey,
                fileName = this.fileName,
                contentType = this.contentType,
                fileSize = this.fileSize
        )
    }

    private fun BookCoverMetadata.toEntity(): BookCoverEntity {
        return BookCoverEntity(
                id = this.id,
                bookId = this.bookId.value,
                storageKey = this.storageKey,
                fileName = this.fileName,
                contentType = this.contentType,
                fileSize = this.fileSize
        )
    }
}
