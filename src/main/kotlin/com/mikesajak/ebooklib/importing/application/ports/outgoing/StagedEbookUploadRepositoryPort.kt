package com.mikesajak.ebooklib.importing.application.ports.outgoing

import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUpload
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadId
import java.time.Instant
import java.util.*

interface StagedEbookUploadRepositoryPort {
    fun save(stagedEbookUpload: StagedEbookUpload): StagedEbookUpload
    fun findById(id: StagedEbookUploadId): StagedEbookUpload?
    fun delete(id: StagedEbookUploadId)
    fun findByExpiryAtBefore(now: Instant): List<StagedEbookUpload>
    fun findAll(): List<StagedEbookUpload>
    fun count(): Long
    fun countByExpiryAtBefore(now: Instant): Long
    fun findAllKeys(): List<String>
}
