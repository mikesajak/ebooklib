package com.mikesajak.ebooklib.infrastructure.security

import com.mikesajak.ebooklib.infrastructure.security.persistence.UserJpaRepository
import jakarta.transaction.Transactional
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
@Transactional
class UserService(
    private val userJpaRepository: UserJpaRepository,
    private val passwordEncoder: PasswordEncoder
) {

    fun changePassword(username: String, currentPassword: String, newPassword: String) {
        require(currentPassword.isNotBlank()) {
            "Current password cannot be empty."
        }
        require(newPassword.isNotBlank() && newPassword.length >= 6) {
            "New password must be at least 6 characters long."
        }

        val userEntity = userJpaRepository.findByUsername(username)
            ?: throw IllegalArgumentException("User $username not found")

        if (!passwordEncoder.matches(currentPassword, userEntity.password)) {
            throw IllegalArgumentException("Current password is incorrect")
        }

        val hashedNewPassword = passwordEncoder.encode(newPassword)
        userEntity.password = hashedNewPassword
        userJpaRepository.save(userEntity)
    }
}
