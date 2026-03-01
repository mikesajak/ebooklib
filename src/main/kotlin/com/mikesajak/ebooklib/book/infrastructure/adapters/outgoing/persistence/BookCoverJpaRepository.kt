package com.mikesajak.ebooklib.book.infrastructure.adapters.outgoing.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface BookCoverJpaRepository : JpaRepository<BookCoverEntity, UUID> {
    fun findByBookId(bookId: UUID): BookCoverEntity?
    fun existsByBookId(bookId: UUID): Boolean
    fun deleteByBookId(bookId: UUID)

    @Query("SELECT SUM(c.fileSize) FROM BookCoverEntity c")
    fun sumFileSize(): Long?
}
