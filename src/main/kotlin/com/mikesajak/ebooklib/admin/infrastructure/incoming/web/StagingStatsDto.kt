package com.mikesajak.ebooklib.admin.infrastructure.incoming.web

data class StagingStatsDto(
    val totalItems: Long,
    val expiredItems: Long
)
