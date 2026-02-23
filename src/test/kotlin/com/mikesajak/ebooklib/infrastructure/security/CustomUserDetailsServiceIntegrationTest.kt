package com.mikesajak.ebooklib.infrastructure.security

import com.mikesajak.ebooklib.config.BaseIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import software.amazon.awssdk.services.s3.S3Client
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = [
    "app.security.enabled=true",
    "spring.liquibase.contexts=schema, test-data"
])
class CustomUserDetailsServiceIntegrationTest : BaseIntegrationTest() {

    @MockitoBean
    lateinit var s3Client: S3Client

    @Autowired
    lateinit var userDetailsService: CustomUserDetailsService

    @Test
    fun `should load admin user with roles`() {
        val userDetails = userDetailsService.loadUserByUsername("admin")

        assertEquals("admin", userDetails.username)
        assertTrue(userDetails.isEnabled)
        val roles = userDetails.authorities.map { it.authority }.toSet()
        assertTrue(roles.contains("ROLE_ADMIN"))
        assertTrue(roles.contains("ROLE_USER"))
    }

    @Test
    fun `should load regular user with role`() {
        val userDetails = userDetailsService.loadUserByUsername("user")

        assertEquals("user", userDetails.username)
        assertTrue(userDetails.isEnabled)
        val roles = userDetails.authorities.map { it.authority }.toSet()
        assertTrue(roles.contains("ROLE_USER"))
    }

    @Test
    fun `should throw exception for non-existent user`() {
        assertFailsWith<UsernameNotFoundException> {
            userDetailsService.loadUserByUsername("nonexistent")
        }
    }
}
