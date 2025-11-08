package com.mikesajak.ebooklib.book.infrastructure.adapters.outgoing.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EbookFormatFileJpaRepository : JpaRepository<EbookFormatFileEntity, UUID> {
    fun findByBookId(bookId: UUID): List<EbookFormatFileEntity>
    fun findByBookIdAndId(bookId: UUID, id: UUID): EbookFormatFileEntity?
    fun deleteByBookIdAndId(bookId: UUID, id: UUID)
}
