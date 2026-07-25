package com.mikesajak.ebooklib.book.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.admin.domain.model.FormatTypeStats
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface EbookFormatFileJpaRepository : JpaRepository<EbookFormatFileEntity, UUID> {
    fun findByBookId(bookId: UUID): List<EbookFormatFileEntity>
    fun findByBookIdAndId(bookId: UUID, id: UUID): EbookFormatFileEntity?
    fun deleteByBookIdAndId(bookId: UUID, id: UUID)

    @Query("SELECT SUM(f.fileSize) FROM EbookFormatFileEntity f")
    fun sumFileSize(): Long?

    @Query("SELECT new com.mikesajak.ebooklib.admin.domain.model.FormatTypeStats(UPPER(f.formatType), COUNT(f), COALESCE(SUM(f.fileSize), 0L)) FROM EbookFormatFileEntity f GROUP BY UPPER(f.formatType)")
    fun getFormatTypeStats(): List<FormatTypeStats>

    @Query("SELECT f.storageKey FROM EbookFormatFileEntity f")
    fun findAllStorageKeys(): List<String>
}
