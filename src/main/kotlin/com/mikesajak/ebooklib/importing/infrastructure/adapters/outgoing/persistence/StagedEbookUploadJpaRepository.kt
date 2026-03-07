package com.mikesajak.ebooklib.importing.infrastructure.adapters.outgoing.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

interface StagedEbookUploadJpaRepository : JpaRepository<StagedEbookUploadEntity, UUID> {
    fun findByExpiryAtBefore(now: Instant): List<StagedEbookUploadEntity>
    fun countAllByExpiryAtBefore(now: Instant): Long
    fun findAllByImportSessionId(importSessionId: UUID): List<StagedEbookUploadEntity>
    
    @Modifying
    @Transactional
    fun deleteAllByImportSessionId(importSessionId: UUID)
    
    fun findAllByResolutionItemId(resolutionItemId: UUID): List<StagedEbookUploadEntity>

    @Query("SELECT 'staged/' || cast(s.id as string) FROM StagedEbookUploadEntity s")
    fun findAllStorageKeys(): List<String>
}
