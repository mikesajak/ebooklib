package com.mikesajak.ebooklib.admin.application.ports.incoming

import com.mikesajak.ebooklib.admin.domain.model.User
import java.util.*

interface UserManagementUseCase {
    fun getAllUsers(): List<User>
    fun deleteUser(id: UUID)
    fun updateUserRoles(id: UUID, roles: Set<String>)
    fun createUser(command: CreateUserCommand): CreatedUserResponse
}
