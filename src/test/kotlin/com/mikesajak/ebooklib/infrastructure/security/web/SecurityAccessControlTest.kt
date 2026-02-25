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

    private fun extractCookie(setCookie: String?): String? =
        setCookie?.substringBefore(";")

    private fun getXsrfTokenFromResponse(response: org.springframework.http.ResponseEntity<*>): Pair<String, String>? {
        val setCookie = response.headers["Set-Cookie"]?.find { it.startsWith("XSRF-TOKEN") }
        if (setCookie == null) return null
        val cookie = extractCookie(setCookie)!!
        val token = cookie.substringAfter("XSRF-TOKEN=")
        if (token.isEmpty()) return null
        return Pair(cookie, token)
    }

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

    @Test
    fun `should return 403 for POST requests without CSRF token when authenticated`() {
        // Get XSRF-TOKEN cookie. Try /api/me first as it definitely goes through filters.
        val initialResponse = restTemplate.getForEntity("/api/me", String::class.java)
        var xsrfInfo = getXsrfTokenFromResponse(initialResponse)
        
        if (xsrfInfo == null) {
            // Try / as backup
            val rootResponse = restTemplate.getForEntity("/", String::class.java)
            xsrfInfo = getXsrfTokenFromResponse(rootResponse)
        }
        
        assertThat(xsrfInfo).withFailMessage("Could not obtain CSRF token").isNotNull()
        val (xsrfCookie, xsrfToken) = xsrfInfo!!

        // Login
        val loginHeaders = org.springframework.http.HttpHeaders()
        loginHeaders.contentType = org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED
        loginHeaders.add("Cookie", xsrfCookie)
        loginHeaders.add("X-XSRF-TOKEN", xsrfToken)
        
        val loginRequest = org.springframework.util.LinkedMultiValueMap<String, String>()
        loginRequest.add("username", "admin")
        loginRequest.add("password", "password")

        val loginResponse = restTemplate.postForEntity("/login", org.springframework.http.HttpEntity(loginRequest, loginHeaders), String::class.java)
        assertThat(loginResponse.statusCode).isEqualTo(HttpStatus.OK)

        val setSessionCookie = loginResponse.headers["Set-Cookie"]?.find { it.startsWith("JSESSIONID") }
        assertThat(setSessionCookie).isNotNull()
        val sessionCookie = extractCookie(setSessionCookie)!!

        // Get fresh XSRF-TOKEN if issued during login
        val freshXsrfInfo = getXsrfTokenFromResponse(loginResponse) ?: xsrfInfo

        // Try to POST without CSRF header
        val postHeaders = org.springframework.http.HttpHeaders()
        postHeaders.add("Cookie", "$sessionCookie; ${freshXsrfInfo.first}")
        postHeaders.contentType = org.springframework.http.MediaType.APPLICATION_JSON
        
        val body = "{\"firstName\": \"Test\", \"lastName\": \"Author\"}"
        val postEntity = org.springframework.http.HttpEntity(body, postHeaders)

        val response = restTemplate.postForEntity("/api/authors", postEntity, String::class.java)
        
        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `should permit POST requests with valid CSRF token when authenticated`() {
        // Get XSRF-TOKEN cookie
        val initialResponse = restTemplate.getForEntity("/api/me", String::class.java)
        var xsrfInfo = getXsrfTokenFromResponse(initialResponse)
        
        if (xsrfInfo == null) {
            val rootResponse = restTemplate.getForEntity("/", String::class.java)
            xsrfInfo = getXsrfTokenFromResponse(rootResponse)
        }
        
        assertThat(xsrfInfo).withFailMessage("Could not obtain CSRF token").isNotNull()
        val (initialXsrfCookie, initialXsrfToken) = xsrfInfo!!

        // Login
        val loginHeaders = org.springframework.http.HttpHeaders()
        loginHeaders.contentType = org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED
        loginHeaders.add("Cookie", initialXsrfCookie)
        loginHeaders.add("X-XSRF-TOKEN", initialXsrfToken)
        
        val loginRequest = org.springframework.util.LinkedMultiValueMap<String, String>()
        loginRequest.add("username", "admin")
        loginRequest.add("password", "password")

        val loginResponse = restTemplate.postForEntity("/login", org.springframework.http.HttpEntity(loginRequest, loginHeaders), String::class.java)
        assertThat(loginResponse.statusCode).isEqualTo(HttpStatus.OK)

        val setSessionCookie = loginResponse.headers["Set-Cookie"]?.find { it.startsWith("JSESSIONID") }
        assertThat(setSessionCookie).isNotNull()
        val sessionCookie = extractCookie(setSessionCookie)!!

        // Get fresh XSRF-TOKEN
        val freshXsrfInfo = getXsrfTokenFromResponse(loginResponse) ?: xsrfInfo

        // Try to POST with CSRF header
        val postHeaders = org.springframework.http.HttpHeaders()
        postHeaders.add("Cookie", "$sessionCookie; ${freshXsrfInfo.first}")
        postHeaders.add("X-XSRF-TOKEN", freshXsrfInfo.second)
        postHeaders.contentType = org.springframework.http.MediaType.APPLICATION_JSON
        
        val body = "{\"firstName\": \"Test\", \"lastName\": \"Author\"}"
        val postEntity = org.springframework.http.HttpEntity(body, postHeaders)

        val response = restTemplate.postForEntity("/api/authors", postEntity, String::class.java)
        
        assertThat(response.statusCode).isNotEqualTo(HttpStatus.FORBIDDEN)
        assertThat(response.statusCode).isNotEqualTo(HttpStatus.UNAUTHORIZED)
    }
}
