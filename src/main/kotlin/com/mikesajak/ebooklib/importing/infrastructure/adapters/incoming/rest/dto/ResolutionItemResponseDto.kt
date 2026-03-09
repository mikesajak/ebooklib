package com.mikesajak.ebooklib.importing.infrastructure.adapters.incoming.rest.dto

import com.mikesajak.ebooklib.importing.domain.model.ResolutionItemStatus
import java.time.Instant

data class ResolutionItemResponseDto(
    val id: String,
    val importSessionId: String,
    val title: String,
    val authors: List<String>,
    val status: ResolutionItemStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val metadataJson: String?,
    val metadata: Map<String, Any?> = emptyMap(),
    val formats: List<ResolutionItemFormatDto>
)

data class ResolutionItemFormatDto(
    val uploadId: String,
    val fileName: String,
    val contentType: String,
    val fileSize: Long
)
