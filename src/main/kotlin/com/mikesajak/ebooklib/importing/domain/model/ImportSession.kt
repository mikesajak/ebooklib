package com.mikesajak.ebooklib.importing.domain.model

import java.time.Instant
import java.util.*

data class ImportSessionId(val value: UUID) {
    override fun toString(): String = value.toString()
}

enum class ImportSessionStatus {
    ACTIVE,
    PROCESSING,
    FINALIZING,
    FINALIZED,
    CANCELLED,
    EXPIRED
}

data class ImportSession(
    val id: ImportSessionId,
    val status: ImportSessionStatus,
    val totalFiles: Int,
    val processedFiles: Int,
    val failedFiles: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
    val expiryAt: Instant
)
