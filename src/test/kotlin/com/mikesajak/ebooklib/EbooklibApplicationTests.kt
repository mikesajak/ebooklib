package com.mikesajak.ebooklib

import com.mikesajak.ebooklib.file.application.ports.outgoing.FileStoragePort
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import software.amazon.awssdk.services.s3.S3Client

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = [
    "minio.endpoint=http://localhost:9000",
    "minio.access-key=testaccesskey",
    "minio.secret-key=testsecretkey",
    "minio.bucket-name=test-bucket,",
    "liquibase.contexts=schema",
    "preliquibase=true"
])
@Testcontainers
class EbooklibApplicationTests {

    companion object {
        @Container
        val postgresContainer = PostgreSQLContainer<Nothing>("postgres:16-alpine").apply {
            withDatabaseName("testdb")
            withUsername("testuser")
            withPassword("testpass")
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgresContainer::getJdbcUrl)
            registry.add("spring.datasource.username", postgresContainer::getUsername)
            registry.add("spring.datasource.password", postgresContainer::getPassword)
        }
    }

    @MockitoBean
    lateinit var fileStoragePort: FileStoragePort

    @MockitoBean
    lateinit var s3Client: S3Client

    @Test
    fun contextLoads() {
    }

}
