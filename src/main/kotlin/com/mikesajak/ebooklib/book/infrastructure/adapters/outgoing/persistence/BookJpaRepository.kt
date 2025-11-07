package com.mikesajak.ebooklib.book.infrastructure.adapters.outgoing.persistence
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface BookJpaRepository : JpaRepository<BookEntity, UUID> {
    @Query("SELECT b FROM BookEntity b JOIN b.authors a WHERE a.id = :authorId")
    fun findBooksByAuthorId(@Param("authorId") authorId: UUID, pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<BookEntity>

    @Query("SELECT b FROM BookEntity b WHERE b.series.id = :seriesId")
    fun findBooksBySeriesId(@Param("seriesId") seriesId: UUID, pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<BookEntity>

    override fun deleteById(id: UUID)
}
