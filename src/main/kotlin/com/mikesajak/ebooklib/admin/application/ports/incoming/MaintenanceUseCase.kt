package com.mikesajak.ebooklib.admin.application.ports.incoming

import com.mikesajak.ebooklib.admin.domain.model.StagingStats
import com.mikesajak.ebooklib.admin.domain.model.StorageScanStats

interface MaintenanceUseCase {
    fun purgeExpiredStaging(): Int
    fun getStagingStats(): StagingStats
    fun startStorageScan()
    fun getStorageScanStats(): StorageScanStats
}
