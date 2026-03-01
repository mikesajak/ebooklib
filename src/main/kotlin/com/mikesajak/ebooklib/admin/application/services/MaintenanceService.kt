package com.mikesajak.ebooklib.admin.application.services

import com.mikesajak.ebooklib.admin.application.ports.incoming.MaintenanceUseCase
import com.mikesajak.ebooklib.admin.domain.model.StagingStats
import com.mikesajak.ebooklib.importing.application.ports.outgoing.StagedEbookUploadRepositoryPort
import com.mikesajak.ebooklib.importing.application.ports.incoming.StagedUploadCleanupUseCase
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class MaintenanceService(
    private val stagedUploadCleanupUseCase: StagedUploadCleanupUseCase,
    private val stagingRepository: StagedEbookUploadRepositoryPort,
    private val storageScanner: AsyncStorageScanner
) : MaintenanceUseCase {

    override fun purgeExpiredStaging(): Int {
        return stagedUploadCleanupUseCase.cleanupExpiredUploads()
    }

    override fun getStagingStats(): StagingStats {
        return StagingStats(
            totalItems = stagingRepository.count(),
            expiredItems = stagingRepository.countByExpiryAtBefore(Instant.now())
        )
    }

    override fun startStorageScan() {
        storageScanner.startScan()
    }

    override fun getStorageScanStats(): StorageScanStats {
        return storageScanner.getLatestStats()
    }
}
