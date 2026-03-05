package com.mikesajak.ebooklib.admin.infrastructure.incoming.web

import com.mikesajak.ebooklib.admin.application.ports.incoming.CreateUserCommand
import com.mikesajak.ebooklib.admin.application.ports.incoming.CreatedUserResponse
import com.mikesajak.ebooklib.admin.application.ports.incoming.UserManagementUseCase
import com.mikesajak.ebooklib.admin.application.ports.incoming.SystemSettingsUseCase
import com.mikesajak.ebooklib.admin.application.ports.incoming.MaintenanceUseCase
import com.mikesajak.ebooklib.importing.application.ports.incoming.MetadataProviderSettingsUseCase
import com.mikesajak.ebooklib.importing.domain.model.MetadataProviderConfig
import com.mikesajak.ebooklib.admin.application.services.AdminStatsService
import com.mikesajak.ebooklib.admin.domain.model.AdminStats
import com.mikesajak.ebooklib.admin.domain.model.StorageScanStats
import com.mikesajak.ebooklib.admin.domain.model.User
import com.mikesajak.ebooklib.admin.domain.model.SystemSetting
import mu.KotlinLogging
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.*

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/admin")
class AdminController(
    private val adminStatsService: AdminStatsService,
    private val userManagementUseCase: UserManagementUseCase,
    private val systemSettingsUseCase: SystemSettingsUseCase,
    private val maintenanceUseCase: MaintenanceUseCase,
    private val metadataProviderSettingsUseCase: MetadataProviderSettingsUseCase
) {

    @GetMapping("/stats")
    fun getStats(authentication: Authentication?): AdminStatsDto {
        logger.info { "Fetching admin stats for user: ${authentication?.name}, authorities: ${authentication?.authorities}" }
        return adminStatsService.getStats().toDto()
    }

    @GetMapping("/users")
    fun getAllUsers(): List<UserDto> {
        return userManagementUseCase.getAllUsers().map { it.toDto() }
    }

    @DeleteMapping("/users/{id}")
    fun deleteUser(@PathVariable id: UUID) {
        userManagementUseCase.deleteUser(id)
    }

    @PutMapping("/users/{id}/roles")
    fun updateUserRoles(@PathVariable id: UUID, @RequestBody roles: Set<String>) {
        userManagementUseCase.updateUserRoles(id, roles)
    }

    @PostMapping("/users")
    fun createUser(@RequestBody command: CreateUserCommand): CreatedUserResponse {
        return userManagementUseCase.createUser(command)
    }

    @GetMapping("/settings")
    fun getAllSettings(): List<SystemSettingsDto> {
        return systemSettingsUseCase.getAllSettings().map { it.toDto() }
    }

    @PutMapping("/settings/{key}")
    fun updateSetting(@PathVariable key: String, @RequestBody value: String?): SystemSettingsDto {
        return systemSettingsUseCase.updateSetting(key, value).toDto()
    }

    @PostMapping("/maintenance/purge-staging")
    fun purgeStaging(): Int {
        return maintenanceUseCase.purgeExpiredStaging()
    }

    @GetMapping("/maintenance/staging-stats")
    fun getStagingStats(): StagingStatsDto {
        val stats = maintenanceUseCase.getStagingStats()
        return StagingStatsDto(stats.totalItems, stats.expiredItems)
    }

    @PostMapping("/maintenance/storage-scan")
    fun startStorageScan() {
        maintenanceUseCase.startStorageScan()
    }

    @GetMapping("/maintenance/storage-scan/stats")
    fun getStorageScanStats(): StorageScanStats {
        return maintenanceUseCase.getStorageScanStats()
    }

    @GetMapping("/metadata-providers")
    fun getMetadataProviders(): List<MetadataProviderConfigDto> {
        return metadataProviderSettingsUseCase.getProvidersConfig().map { it.toDto() }
    }

    @PatchMapping("/metadata-providers/{id}")
    fun updateMetadataProvider(
        @PathVariable id: String,
        @RequestBody request: UpdateMetadataProviderRequest
    ): MetadataProviderConfigDto {
        return metadataProviderSettingsUseCase.updateProviderConfig(id, request.enabled, request.settings).toDto()
    }

    private fun AdminStats.toDto() = AdminStatsDto(
        bookCount = bookCount,
        authorCount = authorCount,
        seriesCount = seriesCount,
        formatCount = formatCount,
        coverCount = coverCount,
        totalFormatSize = totalFormatSize,
        totalCoverSize = totalCoverSize
    )

    private fun User.toDto() = UserDto(
        id = id,
        username = username,
        roles = roles,
        enabled = enabled
    )

    private fun SystemSetting.toDto() = SystemSettingsDto(
        key = key,
        value = value,
        description = description
    )

    private fun MetadataProviderConfig.toDto() = MetadataProviderConfigDto(
        id = id,
        name = name,
        enabled = enabled,
        settings = settings
    )
}
