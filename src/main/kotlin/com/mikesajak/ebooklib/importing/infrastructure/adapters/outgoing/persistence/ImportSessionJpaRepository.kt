package com.mikesajak.ebooklib.importing.infrastructure.adapters.outgoing.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.*

interface ImportSessionJpaRepository : JpaRepository<ImportSessionEntity, UUID> {
    fun findAllByExpiryAtBefore(now: Instant): List<ImportSessionEntity>
    fun findAllByStatus(status: com.mikesajak.ebooklib.importing.domain.model.ImportSessionStatus): List<ImportSessionEntity>

    @Modifying
    @Query("UPDATE ImportSessionEntity s SET s.processedFiles = s.processedFiles + 1, s.updatedAt = :now WHERE s.id = :id")
    fun incrementProcessed(id: UUID, now: Instant)

    @Modifying
    @Query("UPDATE ImportSessionEntity s SET s.failedFiles = s.failedFiles + 1, s.updatedAt = :now WHERE s.id = :id")
    fun incrementFailed(id: UUID, now: Instant)
}
