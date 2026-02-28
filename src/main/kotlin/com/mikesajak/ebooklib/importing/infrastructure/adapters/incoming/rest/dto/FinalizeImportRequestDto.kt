package com.mikesajak.ebooklib.importing.infrastructure.adapters.incoming.rest.dto

import java.time.LocalDate
import java.util.*

data class FinalizeImportRequestDto(
    val uploadId: UUID,
    val bookId: UUID? = null,
    val title: String,
    val authorIds: List<UUID>,
    val publisher: String? = null,
    val publicationDate: LocalDate? = null,
    val description: String? = null,
    val seriesId: UUID? = null,
    val volume: Int? = null,
    val labels: List<String> = emptyList(),
    val updateCover: Boolean = false
)
