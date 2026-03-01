package com.mikesajak.ebooklib.admin.application.ports.incoming

import com.mikesajak.ebooklib.admin.domain.model.User
import java.util.*

interface UserManagementUseCase {
    fun getAllUsers(): List<User>
    fun deleteUser(id: UUID)
    fun updateUserRoles(id: UUID, roles: Set<String>)
    // Create will be refined in Phase 2 tasks (USER-003)
}
