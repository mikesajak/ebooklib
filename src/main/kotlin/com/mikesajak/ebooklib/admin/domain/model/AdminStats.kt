package com.mikesajak.ebooklib.admin.domain.model

data class AdminStats(
    val bookCount: Long,
    val authorCount: Long,
    val seriesCount: Long,
    val formatCount: Long,
    val coverCount: Long
)
