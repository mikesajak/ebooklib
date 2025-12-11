package com.mikesajak.ebooklib

import com.mikesajak.ebooklib.config.BaseIntegrationTest
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileStoragePort
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import software.amazon.awssdk.services.s3.S3Client

@ActiveProfiles("test")
@TestPropertySource(properties = [
    "minio.endpoint=http://localhost:9000",
    "minio.access-key=testaccesskey",
    "minio.secret-key=testsecretkey",
    "minio.bucket-name=test-bucket,",
    "liquibase.contexts=schema",
    "preliquibase=true"
])
class EbooklibApplicationTests : BaseIntegrationTest() {


    @MockitoBean
    lateinit var fileStoragePort: FileStoragePort

    @MockitoBean
    lateinit var s3Client: S3Client

    @Test
    fun contextLoads() {
    }

}
