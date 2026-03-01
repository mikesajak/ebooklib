package com.mikesajak.ebooklib.admin.application.services

import com.mikesajak.ebooklib.admin.application.ports.incoming.UserManagementUseCase
import com.mikesajak.ebooklib.admin.domain.model.User
import com.mikesajak.ebooklib.infrastructure.security.persistence.UserJpaRepository
import com.mikesajak.ebooklib.infrastructure.security.persistence.UserRoleEntity
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.util.*

@Service
@Transactional
class UserManagementService(
    private val userJpaRepository: UserJpaRepository
) : UserManagementUseCase {

    override fun getAllUsers(): List<User> {
        return userJpaRepository.findAll().map { entity ->
            User(
                id = entity.id,
                username = entity.username,
                roles = entity.roles.map { it.role }.toSet(),
                enabled = entity.enabled
            )
        }
    }

    override fun deleteUser(id: UUID) {
        userJpaRepository.deleteById(id)
    }

    override fun updateUserRoles(id: UUID, roles: Set<String>) {
        val userEntity = userJpaRepository.findById(id).orElseThrow { NoSuchElementException("User with id $id not found") }
        
        userEntity.roles.clear()
        roles.forEach { role ->
            userEntity.roles.add(UserRoleEntity(id = UUID.randomUUID(), user = userEntity, role = role))
        }
        
        userJpaRepository.save(userEntity)
    }
}
