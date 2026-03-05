package com.mikesajak.ebooklib.importing.domain.model

data class MetadataProviderConfig(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val settings: Map<String, String> = emptyMap()
)
