package com.mikesajak.ebooklib.file.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.config.BaseIntegrationTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MinIOContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.*

@ActiveProfiles("test")
@Testcontainers
class S3FileStorageAdapterIntegrationTest: BaseIntegrationTest() {

    @Autowired
    private lateinit var s3FileStorageAdapter: S3FileStorageAdapter

    @Autowired
    private lateinit var s3Client: S3Client

    private val bucketName = "test-bucket"

    companion object {
        @Container
        val minioContainer = MinIOContainer("minio/minio:latest")
                .withCommand("server /data --console-address :9001")
                .withEnv("MINIO_ROOT_USER", "minioadmin")
                .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("minio.endpoint") { minioContainer.getS3URL() }
            registry.add("minio.access-key") { minioContainer.userName }
            registry.add("minio.secret-key") { minioContainer.password }
            registry.add("minio.bucket-name") { "test-bucket" }
        }
    }

    @BeforeEach
    fun setUp() {
        // Ensure the bucket exists before each test
        if (!s3Client.listBuckets().buckets().any { it.name() == bucketName }) {
            s3Client.createBucket { it.bucket(bucketName) }
        }
        // Clear the bucket before each test
        s3Client.listObjectsV2 { it.bucket(bucketName) }.contents().forEach { s3Object ->
            s3Client.deleteObject { it.bucket(bucketName).key(s3Object.key()) }
        }
    }

    @Test
    fun `uploadFile should upload a file to MinIO`() {
        // Given
        val fileContent = "Hello, MinIO!".toByteArray(StandardCharsets.UTF_8)
        val originalFileName = "test-upload.txt"
        val contentType = "text/plain"
        val inputStream = ByteArrayInputStream(fileContent)

        // When
        val fileMetadata = s3FileStorageAdapter.uploadFile(inputStream, originalFileName, contentType)

        // Then
        assertNotNull(fileMetadata.id)
        assertEquals(originalFileName, fileMetadata.fileName)
        assertEquals(contentType, fileMetadata.contentType)
        assertEquals(fileContent.size.toLong(), fileMetadata.size)

        // Verify file exists in MinIO
        val downloadedContent =
            s3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(fileMetadata.id).build())
        assertArrayEquals(fileContent, downloadedContent.readAllBytes())
    }

    @Test
    fun `downloadFile should download a file from MinIO`() {
        // Given
        val fileContent = "Downloadable content.".toByteArray(StandardCharsets.UTF_8)
        val originalFileName = "test-download.txt"
        val contentType = "text/plain"
        val inputStream = ByteArrayInputStream(fileContent)

        val uploadedMetadata = s3FileStorageAdapter.uploadFile(inputStream, originalFileName, contentType)

        // When
        val downloadedStream = s3FileStorageAdapter.downloadFile(uploadedMetadata.id)

        // Then
        assertArrayEquals(fileContent, downloadedStream.readAllBytes())
    }

    @Test
    fun `downloadFile should throw NoSuchKeyException for non-existent file`() {
        // Given
        val nonExistentFileId = UUID.randomUUID().toString()

        // When & Then
        assertThrows(NoSuchKeyException::class.java) {
            s3FileStorageAdapter.downloadFile(nonExistentFileId)
        }
    }

    @Test
    fun `deleteFile should delete a file from MinIO`() {
        // Given
        val fileContent = "Content to be deleted.".toByteArray(StandardCharsets.UTF_8)
        val originalFileName = "test-delete.txt"
        val contentType = "text/plain"
        val inputStream = ByteArrayInputStream(fileContent)

        val uploadedMetadata = s3FileStorageAdapter.uploadFile(inputStream, originalFileName, contentType)

        // When
        s3FileStorageAdapter.deleteFile(uploadedMetadata.id)

        // Then
        assertThrows(NoSuchKeyException::class.java) {
            s3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(uploadedMetadata.id).build())
        }
    }

    @Test
    fun `deleteFile should not throw exception for non-existent file`() {
        // Given
        val nonExistentFileId = UUID.randomUUID().toString()

        // When & Then
        assertDoesNotThrow {
            s3FileStorageAdapter.deleteFile(nonExistentFileId)
        }
    }

    @Test
    fun `getFileMetadata should return metadata for existing file`() {
        // Given
        val fileContent = "Metadata content.".toByteArray(StandardCharsets.UTF_8)
        val originalFileName = "test-metadata.txt"
        val contentType = "text/plain"
        val inputStream = ByteArrayInputStream(fileContent)

        val uploadedMetadata = s3FileStorageAdapter.uploadFile(inputStream, originalFileName, contentType)

        // When
        val retrievedMetadata = s3FileStorageAdapter.getFileMetadata(uploadedMetadata.id)

        // Then
        assertNotNull(retrievedMetadata)
        assertEquals(uploadedMetadata.id, retrievedMetadata!!.id)
        // Note: originalFileName is not stored in S3 metadata by default, so we assert against fileId
        assertEquals(uploadedMetadata.id, retrievedMetadata.fileName)
        assertEquals(uploadedMetadata.contentType, retrievedMetadata.contentType)
        assertEquals(uploadedMetadata.size, retrievedMetadata.size)
    }

    @Test
    fun `getFileMetadata should return null for non-existent file`() {
        // Given
        val nonExistentFileId = UUID.randomUUID().toString()

        // When
        val retrievedMetadata = s3FileStorageAdapter.getFileMetadata(nonExistentFileId)

        // Then
        assertNull(retrievedMetadata)
    }
}
