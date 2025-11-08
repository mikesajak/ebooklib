package com.mikesajak.ebooklib.infrastructure.config


import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import java.net.URI

private val logger = KotlinLogging.logger {}

@Configuration
class MinioConfig(
    @Value("\${minio.endpoint}") private val minioEndpoint: String,
    @Value("\${minio.access-key}") private val minioAccessKey: String,
    @Value("\${minio.secret-key}") private val minioSecretKey: String,
    @Value("\${minio.bucket-name:ebook-library-files}") private val bucketName: String
) {

    @Bean
    fun s3Client(): S3Client {
        val s3Client = S3Client.builder()
            .endpointOverride(URI.create(minioEndpoint))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(minioAccessKey, minioSecretKey)
                )
            )
            .region(Region.US_EAST_1) // MinIO doesn't strictly use regions, but a default is required
            .forcePathStyle(true) // Crucial for MinIO compatibility
            .build()

        // Ensure the bucket exists
        if (!s3Client.listBuckets().buckets().any { bucket -> bucket.name() == bucketName }) {
            try {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build())
                logger.info { "MinIO bucket '$bucketName' created." }
            } catch (e: S3Exception) {
                if (e.statusCode() == 409 || e.statusCode() == 400 && e.message?.contains("BucketAlreadyOwnedByYou") == true) {
                    logger.info { "MinIO bucket '$bucketName' already exists." }
                } else {
                    logger.error(e) { "Error creating MinIO bucket '$bucketName'." }
                    throw e
                }
            }
        }
        return s3Client
    }
}
