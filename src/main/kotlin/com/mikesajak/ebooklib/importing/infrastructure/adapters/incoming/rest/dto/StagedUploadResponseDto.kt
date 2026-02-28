package com.mikesajak.ebooklib.importing.infrastructure.adapters.incoming.rest.dto

import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadStatus
import java.time.Instant
import java.util.*

data class StagedUploadResponseDto(
    val id: String,
    val fileName: String,
    val contentType: String,
    val fileSize: Long,
    val metadata: Map<String, Any?>,
    val validation: StagedUploadValidationDto? = null,
    val status: StagedEbookUploadStatus,
    val createdAt: Instant,
    val expiryAt: Instant
)

data class StagedUploadValidationDto(
    val candidates: List<MatchCandidateDto>
)

data class MatchCandidateDto(
    val bookId: UUID,
    val title: String,
    val authors: List<String>,
    val titleMatch: Boolean,
    val authorMatch: Boolean,
    val duplicateFormat: Boolean,
    val score: Int
)
