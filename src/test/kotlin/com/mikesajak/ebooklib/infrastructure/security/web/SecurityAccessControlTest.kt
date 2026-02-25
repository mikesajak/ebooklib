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
@TestPropertySource(properties = ["app.security.enabled=true"])
class SecurityAccessControlTest : BaseIntegrationTest() {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @MockitoBean
    lateinit var fileStoragePort: FileStoragePort

    @MockitoBean
    lateinit var s3Client: S3Client

    @Test
    fun `should return 401 for unauthenticated access to api endpoints`() {
        val endpoints = listOf(
            "/api/authors",
            "/api/books",
            "/api/series",
            "/api/me"
        )

        for (endpoint in endpoints) {
            val response = restTemplate.getForEntity(endpoint, String::class.java)
            assertThat(response.statusCode)
                .withFailMessage("Endpoint $endpoint should be secured")
                .isEqualTo(HttpStatus.UNAUTHORIZED)

            // Acceptance Criteria #2: Response body is empty or contains standard JSON error response
            // When using HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), the body is usually empty
            // unless customized.
            if (response.body != null) {
                assertThat(response.body).satisfiesAnyOf(
                    { assertThat(it).isNullOrEmpty() },
                    { assertThat(it).contains("\"status\":401") },
                    { assertThat(it).contains("\"error\":\"Unauthorized\"") }
                )
            }
        }
    }

    @Test
    fun `should permit access to public endpoints even when security is enabled`() {
        val publicEndpoints = listOf(
            "/",
            "/index.html",
            "/favicon.ico"
        )

        for (endpoint in publicEndpoints) {
            val response = restTemplate.getForEntity(endpoint, String::class.java)
            assertThat(response.statusCode)
                .withFailMessage("Endpoint $endpoint should be public")
                .isEqualTo(HttpStatus.OK)
        }
    }
}
