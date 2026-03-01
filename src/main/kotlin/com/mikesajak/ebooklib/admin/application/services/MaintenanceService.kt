package com.mikesajak.ebooklib.admin.application.services

import com.mikesajak.ebooklib.admin.application.ports.incoming.MaintenanceUseCase
import com.mikesajak.ebooklib.importing.application.ports.incoming.StagedUploadCleanupUseCase
import org.springframework.stereotype.Service

@Service
class MaintenanceService(
    private val stagedUploadCleanupUseCase: StagedUploadCleanupUseCase
) : MaintenanceUseCase {

    override fun purgeExpiredStaging(): Int {
        return stagedUploadCleanupUseCase.cleanupExpiredUploads()
    }
}
