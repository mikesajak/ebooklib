package com.mikesajak.ebooklib.admin.infrastructure.incoming.web

data class MetadataProviderConfigDto(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val settings: Map<String, String> = emptyMap()
)

data class UpdateMetadataProviderRequest(
    val enabled: Boolean,
    val settings: Map<String, String> = emptyMap()
)
