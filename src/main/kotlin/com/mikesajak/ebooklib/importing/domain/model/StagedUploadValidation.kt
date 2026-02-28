package com.mikesajak.ebooklib.importing.domain.model

import java.util.*

data class StagedUploadValidation(
    val candidates: List<MatchCandidate> = emptyList()
)

data class MatchCandidate(
    val bookId: UUID,
    val title: String,
    val authors: List<String>,
    val titleMatch: Boolean,
    val authorMatch: Boolean,
    val score: Int = 0
)
