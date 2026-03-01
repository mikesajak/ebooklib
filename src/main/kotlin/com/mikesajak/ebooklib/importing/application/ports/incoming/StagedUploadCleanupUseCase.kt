package com.mikesajak.ebooklib.importing.application.ports.incoming

interface StagedUploadCleanupUseCase {
    fun cleanupExpiredUploads(): Int
}
