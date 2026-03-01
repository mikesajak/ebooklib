package com.mikesajak.ebooklib.admin.application.ports.incoming

interface MaintenanceUseCase {
    fun purgeExpiredStaging(): Int
}
