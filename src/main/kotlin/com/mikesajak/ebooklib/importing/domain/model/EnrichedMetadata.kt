package com.mikesajak.ebooklib.importing.domain.model

import java.time.LocalDate

data class EnrichedMetadata(
    val providerId: String,
    val title: String?,
    val authors: List<String> = emptyList(),
    val isbns: List<String> = emptyList(),
    val description: String?,
    val publisher: String?,
    val publicationDate: LocalDate?,
    val coverUrl: String?,
    val series: String?,
    val volume: Int?
)
