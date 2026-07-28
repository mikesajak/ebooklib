package com.mikesajak.ebooklib.infrastructure.security

import com.mikesajak.ebooklib.infrastructure.security.persistence.UserEntity
import com.mikesajak.ebooklib.infrastructure.security.persistence.UserJpaRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*

class UserServiceTest {

    private val userJpaRepository = mock(UserJpaRepository::class.java)
    private val passwordEncoder = mock(PasswordEncoder::class.java)
    private val userService = UserService(userJpaRepository, passwordEncoder)

    @Test
    fun `should change password successfully when current password is correct`() {
        val username = "testuser"
        val currentPassword = "oldPassword123"
        val newPassword = "newPassword456"
        val hashedOldPassword = "hashedOldPassword"
        val hashedNewPassword = "hashedNewPassword"

        val userEntity = UserEntity(
            id = UUID.randomUUID(),
            username = username,
            password = hashedOldPassword,
            enabled = true
        )

        `when`(userJpaRepository.findByUsername(username)).thenReturn(userEntity)
        `when`(passwordEncoder.matches(currentPassword, hashedOldPassword)).thenReturn(true)
        `when`(passwordEncoder.encode(newPassword)).thenReturn(hashedNewPassword)

        userService.changePassword(username, currentPassword, newPassword)

        assertEquals(hashedNewPassword, userEntity.password)
        verify(userJpaRepository).save(userEntity)
    }

    @Test
    fun `should throw exception when current password is incorrect`() {
        val username = "testuser"
        val currentPassword = "wrongPassword"
        val newPassword = "newPassword456"
        val hashedOldPassword = "hashedOldPassword"

        val userEntity = UserEntity(
            id = UUID.randomUUID(),
            username = username,
            password = hashedOldPassword,
            enabled = true
        )

        `when`(userJpaRepository.findByUsername(username)).thenReturn(userEntity)
        `when`(passwordEncoder.matches(currentPassword, hashedOldPassword)).thenReturn(false)

        val exception = assertThrows<IllegalArgumentException> {
            userService.changePassword(username, currentPassword, newPassword)
        }

        assertEquals("Current password is incorrect", exception.message)
        verify(userJpaRepository, never()).save(any())
    }

    @Test
    fun `should throw exception when new password is too short`() {
        val exception = assertThrows<IllegalArgumentException> {
            userService.changePassword("testuser", "oldPassword", "123")
        }

        assertTrue(exception.message!!.contains("at least 6 characters"))
    }
}
