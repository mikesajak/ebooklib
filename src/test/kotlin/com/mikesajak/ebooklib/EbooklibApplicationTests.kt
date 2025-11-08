package com.mikesajak.ebooklib

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.boot.test.mock.mockito.MockBean
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileStoragePort

@SpringBootTest
@TestPropertySource(properties = [
    "minio.endpoint=http://localhost:9000",
    "minio.access-key=testaccesskey",
    "minio.secret-key=testsecretkey",
    "minio.bucket-name=test-bucket"
])
class EbooklibApplicationTests {

    @MockBean
    lateinit var fileStoragePort: FileStoragePort

	@Test
	fun contextLoads() {
	}

}
