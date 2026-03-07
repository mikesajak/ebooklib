package com.mikesajak.ebooklib.importing.infrastructure.adapters.incoming.rest.dto

import com.mikesajak.ebooklib.importing.domain.model.ImportSessionStatus
import java.time.Instant

data class ImportSessionResponseDto(
    val id: String,
    val status: ImportSessionStatus,
    val totalFiles: Int,
    val processedFiles: Int,
    val failedFiles: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
    val expiryAt: Instant
)
