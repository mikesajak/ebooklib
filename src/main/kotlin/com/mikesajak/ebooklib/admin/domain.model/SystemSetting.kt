package com.mikesajak.ebooklib.admin.domain.model

data class SystemSetting(
    val key: String,
    val value: String?,
    val description: String?
)
