package com.mikesajak.ebooklib.admin.domain.model

import java.time.Instant

data class StorageScanStats(
    val status: ScanStatus,
    val startedAt: Instant?,
    val finishedAt: Instant?,
    val totalFilesScanned: Int,
    val totalScannedSize: Long = 0,
    val orphanedFilesFound: Int,
    val orphanedSize: Long = 0,
    val orphanedFileKeys: List<String>,
    val progressPercent: Int,
    val error: String? = null
)

enum class ScanStatus {
    IDLE, RUNNING, PURGING, COMPLETED, FAILED
}
