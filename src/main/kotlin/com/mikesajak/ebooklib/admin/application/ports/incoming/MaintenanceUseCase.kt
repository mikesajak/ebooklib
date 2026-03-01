package com.mikesajak.ebooklib.admin.application.ports.incoming

import com.mikesajak.ebooklib.admin.domain.model.StagingStats

interface MaintenanceUseCase {
    fun purgeExpiredStaging(): Int
    fun getStagingStats(): StagingStats
}
