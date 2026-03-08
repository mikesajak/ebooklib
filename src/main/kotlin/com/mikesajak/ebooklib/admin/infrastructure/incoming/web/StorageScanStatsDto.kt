package com.mikesajak.ebooklib.admin.infrastructure.incoming.web

import com.mikesajak.ebooklib.admin.domain.model.ScanStatus
import java.time.Instant

data class StorageScanStatsDto(
    val status: ScanStatus,
    val startedAt: Instant?,
    val finishedAt: Instant?,
    val totalFilesScanned: Int,
    val totalScannedSize: Long,
    val orphanedFilesFound: Int,
    val orphanedSize: Long,
    val orphanedFileKeys: List<String>,
    val progressPercent: Int,
    val error: String? = null
)
