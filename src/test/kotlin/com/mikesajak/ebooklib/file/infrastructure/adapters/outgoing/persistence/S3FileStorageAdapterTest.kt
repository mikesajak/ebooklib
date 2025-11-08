package com.mikesajak.ebooklib.file.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.file.application.ports.outgoing.FileMetadata
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MinIOContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URI

@SpringBootTest
@Testcontainers
class S3FileStorageAdapterTest {

    companion object {
        @Container
        val minioContainer = MinIOContainer("minio/minio:latest")
            .withCommand("minio server /data")
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("minio.endpoint") { minioContainer.getS3URL() }
            registry.add("minio.access-key") { "minioadmin" }
            registry.add("minio.secret-key") { "minioadmin" }
            registry.add("minio.bucket-name") { "test-bucket" }
        }
    }

    @Autowired
    private lateinit var s3Client: S3Client

    private lateinit var s3FileStorageAdapter: S3FileStorageAdapter
    private val testBucketName = "test-bucket"

    @BeforeEach
    fun setUp() {
        s3FileStorageAdapter = S3FileStorageAdapter(s3Client, testBucketName)
        // Ensure the bucket is created before each test
        if (!s3Client.listBuckets().buckets().any { it.name() == testBucketName }) {
            s3Client.createBucket { it.bucket(testBucketName) }
        }
    }

    @AfterEach
    fun tearDown() {
        // Clean up objects in the bucket after each test
        s3Client.listObjectsV2 { it.bucket(testBucketName) }.contents().forEach { s3Object ->
            s3Client.deleteObject { it.bucket(testBucketName).key(s3Object.key()) }
        }
        // Delete the bucket itself
        s3Client.deleteBucket { it.bucket(testBucketName) }
    }

    @Test
    fun `should upload and download file`() {
        val content = "Hello MinIO!"
        val inputStream: InputStream = ByteArrayInputStream(content.toByteArray())
        val originalFileName = "testfile.txt"
        val contentType = "text/plain"

        val fileMetadata = s3FileStorageAdapter.uploadFile(inputStream, originalFileName, contentType)

        assertNotNull(fileMetadata.id)
        assertEquals(originalFileName, fileMetadata.fileName)
        assertEquals(contentType, fileMetadata.contentType)
        assertEquals(content.length.toLong(), fileMetadata.size)

        val downloadedStream = s3FileStorageAdapter.downloadFile(fileMetadata.id)
        val downloadedContent = downloadedStream.readBytes().toString(Charsets.UTF_8)

        assertEquals(content, downloadedContent)
    }

    @Test
    fun `should get file metadata`() {
        val content = "Metadata test"
        val inputStream: InputStream = ByteArrayInputStream(content.toByteArray())
        val originalFileName = "metadata.txt"
        val contentType = "text/plain"

        val uploadedMetadata = s3FileStorageAdapter.uploadFile(inputStream, originalFileName, contentType)

        val retrievedMetadata = s3FileStorageAdapter.getFileMetadata(uploadedMetadata.id)

        assertNotNull(retrievedMetadata)
        assertEquals(uploadedMetadata.id, retrievedMetadata?.id)
        // Note: S3 does not store originalFileName, so we expect the storageKey (id) here
        assertEquals(uploadedMetadata.id, retrievedMetadata?.fileName)
        assertEquals(contentType, retrievedMetadata?.contentType)
        assertEquals(content.length.toLong(), retrievedMetadata?.size)
    }

    @Test
    fun `should delete file`() {
        val content = "Delete test"
        val inputStream: InputStream = ByteArrayInputStream(content.toByteArray())
        val originalFileName = "delete.txt"
        val contentType = "text/plain"

        val fileMetadata = s3FileStorageAdapter.uploadFile(inputStream, originalFileName, contentType)
        assertNotNull(s3FileStorageAdapter.getFileMetadata(fileMetadata.id))

        s3FileStorageAdapter.deleteFile(fileMetadata.id)

        assertNull(s3FileStorageAdapter.getFileMetadata(fileMetadata.id))
    }

    @Test
    fun `should return null for non-existent file metadata`() {
        val nonExistentFileId = "non-existent-id"
        val retrievedMetadata = s3FileStorageAdapter.getFileMetadata(nonExistentFileId)
        assertNull(retrievedMetadata)
    }
}
