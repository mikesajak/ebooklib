package com.mikesajak.ebooklib.importing.infrastructure.adapters.incoming.rest.dto

import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadStatus
import java.time.Instant

data class StagedUploadResponseDto(
    val id: String,
    val fileName: String,
    val contentType: String,
    val fileSize: Long,
    val metadata: Map<String, Any?>,
    val status: StagedEbookUploadStatus,
    val createdAt: Instant,
    val expiryAt: Instant
)
