package com.mikesajak.ebooklib.admin.infrastructure.incoming.web

data class AdminStatsDto(
    val bookCount: Long,
    val authorCount: Long,
    val seriesCount: Long,
    val formatCount: Long,
    val coverCount: Long
)
