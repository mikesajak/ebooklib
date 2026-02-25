package com.mikesajak.ebooklib.infrastructure.security.web

import com.mikesajak.ebooklib.config.BaseIntegrationTest
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileStoragePort
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import software.amazon.awssdk.services.s3.S3Client

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = ["app.security.enabled=false"])
class SecurityBypassTest : BaseIntegrationTest() {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @MockitoBean
    lateinit var fileStoragePort: FileStoragePort

    @MockitoBean
    lateinit var s3Client: S3Client

    @Test
    fun `should permit access to all endpoints when security is disabled`() {
        val endpoints = listOf(
            "/api/authors",
            "/api/books",
            "/api/series",
            "/api/me",
            "/",
            "/index.html"
        )

        for (endpoint in endpoints) {
            val response = restTemplate.getForEntity(endpoint, String::class.java)
            // Even if it returns 404 or something else due to missing data, it should NOT be 401 or 403
            assertThat(response.statusCode).isNotEqualTo(HttpStatus.UNAUTHORIZED)
            assertThat(response.statusCode).isNotEqualTo(HttpStatus.FORBIDDEN)
        }
    }

    @Test
    fun `should return dev-user info when security is disabled`() {
        val response = restTemplate.getForEntity("/api/me", String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).contains("\"username\":\"dev-user\"")
        assertThat(response.body).contains("ROLE_ADMIN")
        assertThat(response.body).contains("ROLE_USER")
    }

    @Test
    fun `should bypass CSRF protection when security is disabled`() {
        // Try to POST without any cookies or headers
        val headers = org.springframework.http.HttpHeaders()
        headers.contentType = org.springframework.http.MediaType.APPLICATION_JSON
        val body = """{"firstName": "Test", "lastName": "Author"}"""
        val entity = org.springframework.http.HttpEntity(body, headers)

        val response = restTemplate.postForEntity("/api/authors", entity, String::class.java)
        
        // Should NOT be 403 Forbidden even without CSRF token
        assertThat(response.statusCode).isNotEqualTo(HttpStatus.FORBIDDEN)
        // Also should not be 401
        assertThat(response.statusCode).isNotEqualTo(HttpStatus.UNAUTHORIZED)
    }
}
