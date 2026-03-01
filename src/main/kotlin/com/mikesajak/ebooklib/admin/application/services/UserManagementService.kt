package com.mikesajak.ebooklib.admin.application.services

import com.mikesajak.ebooklib.admin.application.ports.incoming.CreateUserCommand
import com.mikesajak.ebooklib.admin.application.ports.incoming.CreatedUserResponse
import com.mikesajak.ebooklib.admin.application.ports.incoming.UserManagementUseCase
import com.mikesajak.ebooklib.admin.domain.model.User
import com.mikesajak.ebooklib.infrastructure.security.persistence.UserEntity
import com.mikesajak.ebooklib.infrastructure.security.persistence.UserJpaRepository
import com.mikesajak.ebooklib.infrastructure.security.persistence.UserRoleEntity
import jakarta.transaction.Transactional
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.*

@Service
@Transactional
class UserManagementService(
    private val userJpaRepository: UserJpaRepository,
    private val passwordEncoder: PasswordEncoder
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

    override fun createUser(command: CreateUserCommand): CreatedUserResponse {
        if (userJpaRepository.findByUsername(command.username) != null) {
            throw IllegalArgumentException("User with username ${command.username} already exists")
        }

        val initialPassword = generateRandomPassword()
        val hashedPassword = passwordEncoder.encode(initialPassword)

        val userEntity = UserEntity(
            id = UUID.randomUUID(),
            username = command.username,
            password = hashedPassword,
            enabled = true,
            roles = mutableSetOf()
        )

        command.roles.forEach { role ->
            userEntity.roles.add(UserRoleEntity(id = UUID.randomUUID(), user = userEntity, role = role))
        }

        val savedUser = userJpaRepository.save(userEntity)

        return CreatedUserResponse(
            id = savedUser.id!!,
            username = savedUser.username,
            initialPassword = initialPassword
        )
    }

    private fun generateRandomPassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*"
        val random = SecureRandom()
        return (1..12)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }
}
