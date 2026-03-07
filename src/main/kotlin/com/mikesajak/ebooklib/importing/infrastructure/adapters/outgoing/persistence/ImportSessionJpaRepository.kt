package com.mikesajak.ebooklib.importing.infrastructure.adapters.outgoing.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.*

interface ImportSessionJpaRepository : JpaRepository<ImportSessionEntity, UUID> {
    fun findAllByExpiryAtBefore(now: Instant): List<ImportSessionEntity>
    fun findAllByStatus(status: com.mikesajak.ebooklib.importing.domain.model.ImportSessionStatus): List<ImportSessionEntity>
}
