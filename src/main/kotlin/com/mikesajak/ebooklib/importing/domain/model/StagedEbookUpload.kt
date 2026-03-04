package com.mikesajak.ebooklib.importing.domain.model

import java.time.Instant
import java.util.*

data class StagedEbookUploadId(val value: UUID) {
    override fun toString(): String = value.toString()
}

enum class StagedEbookUploadStatus {
    STAGED,
    PROCESSING,
    PARSED,
    FAILED,
    PROMOTED
}

data class StagedEbookUpload(
    val id: StagedEbookUploadId,
    val fileName: String,
    val contentType: String,
    val fileSize: Long,
    val metadataJson: String?,
    val status: StagedEbookUploadStatus,
    val createdAt: Instant,
    val expiryAt: Instant,
    val importSessionId: ImportSessionId? = null,
    val resolutionItemId: UUID? = null
)
