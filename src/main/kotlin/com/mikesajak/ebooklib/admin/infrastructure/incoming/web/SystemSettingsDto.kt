package com.mikesajak.ebooklib.admin.infrastructure.incoming.web

data class SystemSettingsDto(
    val key: String,
    val value: String?,
    val description: String?
)
