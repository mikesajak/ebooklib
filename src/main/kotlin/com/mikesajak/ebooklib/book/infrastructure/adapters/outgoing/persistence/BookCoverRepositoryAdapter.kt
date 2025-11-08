package com.mikesajak.ebooklib.book.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.book.application.ports.outgoing.BookCoverRepositoryPort
import com.mikesajak.ebooklib.book.domain.model.BookCover
import com.mikesajak.ebooklib.book.domain.model.BookId
import org.springframework.stereotype.Component

@Component
class BookCoverRepositoryAdapter(
    private val bookCoverJpaRepository: BookCoverJpaRepository
) : BookCoverRepositoryPort {
    override fun findByBookId(bookId: BookId): BookCover? {
        return bookCoverJpaRepository.findByBookId(bookId.value)?.toDomain()
    }

    override fun save(bookCover: BookCover): BookCover {
        return bookCoverJpaRepository.save(bookCover.toEntity()).toDomain()
    }

    override fun delete(bookCover: BookCover) {
        bookCoverJpaRepository.delete(bookCover.toEntity())
    }

    override fun existsByBookId(bookId: BookId): Boolean {
        return bookCoverJpaRepository.existsByBookId(bookId.value)
    }

    private fun BookCoverEntity.toDomain(): BookCover {
        return BookCover(
            id = this.id,
            bookId = BookId(this.bookId),
            storageKey = this.storageKey,
            fileName = this.fileName,
            contentType = this.contentType,
            fileSize = this.fileSize
        )
    }

    private fun BookCover.toEntity(): BookCoverEntity {
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
