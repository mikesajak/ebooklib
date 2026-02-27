package com.mikesajak.ebooklib.importing.domain.model

data class StagedUploadValidation(
    val titleMatch: Boolean,
    val authorMatch: Boolean,
    val targetBookTitle: String? = null,
    val targetBookAuthors: List<String>? = null
)
