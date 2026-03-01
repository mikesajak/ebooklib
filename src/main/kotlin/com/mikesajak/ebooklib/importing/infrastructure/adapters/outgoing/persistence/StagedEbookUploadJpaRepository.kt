package com.mikesajak.ebooklib.importing.infrastructure.adapters.outgoing.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.*

interface StagedEbookUploadJpaRepository : JpaRepository<StagedEbookUploadEntity, UUID> {
    fun findByExpiryAtBefore(now: Instant): List<StagedEbookUploadEntity>
    fun countByExpiryAtBefore(now: Instant): Long

    @Query("SELECT 'staged/' || cast(s.id as string) FROM StagedEbookUploadEntity s")
    fun findAllStorageKeys(): List<String>
}
