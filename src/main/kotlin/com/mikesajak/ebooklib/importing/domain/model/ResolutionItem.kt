package com.mikesajak.ebooklib.importing.domain.model

import java.time.Instant
import java.util.*

data class ResolutionItemId(val value: UUID) {
    override fun toString(): String = value.toString()
}

enum class ResolutionItemStatus {
    QUEUED,
    UNRESOLVED,
    RESOLVED,
    IGNORED,
    ERROR,
    PROCESSING,
    STAGED
}

data class ResolutionItem(
    val id: ResolutionItemId,
    val importSessionId: ImportSessionId,
    val title: String,
    val authors: List<String>,
    val status: ResolutionItemStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val metadataJson: String? = null
)
