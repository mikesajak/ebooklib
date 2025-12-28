package com.mikesajak.ebooklib.infrastructure.exception

import com.mikesajak.ebooklib.config.BaseIntegrationTest
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileStoragePort
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoBean
import software.amazon.awssdk.services.s3.S3Client
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GlobalExceptionHandlerIntegrationTest : BaseIntegrationTest() {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @MockitoBean
    lateinit var fileStoragePort: FileStoragePort

    @MockitoBean
    lateinit var s3Client: S3Client

    @Test
    fun `should return 404 for unknown endpoint`() {
        val responseDeep = restTemplate.getForEntity("/api/deep/unknown/endpoint", String::class.java)
        assertThat(responseDeep.statusCode).isEqualTo(HttpStatus.NOT_FOUND)

        val responseShallow = restTemplate.getForEntity("/api/unknown", String::class.java)
        assertThat(responseShallow.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `should forward non-api unknown endpoints to index html`() {
        val response = restTemplate.getForEntity("/some-gui-path", String::class.java)
        
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `should return 406 for unacceptable media type`() {
        val headers = org.springframework.http.HttpHeaders()
        headers.accept = listOf(org.springframework.http.MediaType.APPLICATION_PDF)
        val entity = org.springframework.http.HttpEntity<String>(headers)

        val response = restTemplate.exchange("/api/authors", org.springframework.http.HttpMethod.GET, entity, String::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_ACCEPTABLE)
    }

    @Test
    fun `should return 415 for unsupported media type`() {
        val headers = org.springframework.http.HttpHeaders()
        headers.contentType = org.springframework.http.MediaType.APPLICATION_PDF
        val entity = org.springframework.http.HttpEntity<String>("some body", headers)

        val response = restTemplate.exchange("/api/authors", org.springframework.http.HttpMethod.POST, entity, String::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    }

    @Test
    fun `should return 405 for method not allowed`() {
        // /api/authors exists but might not support POST (depends on implementation, but likely doesn't support DELETE if not implemented)
        val response = restTemplate.exchange("/api/authors", org.springframework.http.HttpMethod.DELETE, null, String::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
    }
}
