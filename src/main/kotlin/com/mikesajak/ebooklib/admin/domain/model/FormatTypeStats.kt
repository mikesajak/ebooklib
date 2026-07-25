package com.mikesajak.ebooklib.admin.domain.model

data class FormatTypeStats(
    val formatType: String,
    val count: Long,
    val totalSize: Long
)
