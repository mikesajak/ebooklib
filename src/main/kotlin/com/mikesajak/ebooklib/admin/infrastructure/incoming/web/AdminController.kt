package com.mikesajak.ebooklib.admin.infrastructure.incoming.web

import com.mikesajak.ebooklib.admin.application.ports.incoming.UserManagementUseCase
import com.mikesajak.ebooklib.admin.application.services.AdminStatsService
import com.mikesajak.ebooklib.admin.domain.model.AdminStats
import com.mikesajak.ebooklib.admin.domain.model.User
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/admin")
class AdminController(
    private val adminStatsService: AdminStatsService,
    private val userManagementUseCase: UserManagementUseCase
) {

    @GetMapping("/stats")
    fun getStats(): AdminStatsDto {
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
}
