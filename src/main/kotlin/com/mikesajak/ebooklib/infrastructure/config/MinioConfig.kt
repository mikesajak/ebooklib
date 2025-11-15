package com.mikesajak.ebooklib.infrastructure.config

import mu.KotlinLogging
import org.springframework.boot.context.properties.EnableConfigurationProperties
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
@EnableConfigurationProperties(MinioProperties::class)
class MinioConfig(private val minioProperties: MinioProperties) {

    @Bean
    fun s3Client(): S3Client {
        val s3Client = S3Client.builder()
                .endpointOverride(URI.create(minioProperties.endpoint))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(minioProperties.accessKey, minioProperties.secretKey)
                        )
                )
                .region(Region.US_EAST_1) // MinIO doesn't strictly use regions, but a default is required
                .forcePathStyle(true) // Crucial for MinIO compatibility
                .build()

        // Ensure the bucket exists
        if (!s3Client.listBuckets().buckets().any { bucket -> bucket.name() == minioProperties.bucketName }) {
            try {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(minioProperties.bucketName).build())
                logger.info { "MinIO bucket '${minioProperties.bucketName}' created." }
            } catch (e: S3Exception) {
                if (e.statusCode() == 409 || e.statusCode() == 400 && e.message?.contains("BucketAlreadyOwnedByYou") == true) {
                    logger.info { "MinIO bucket '${minioProperties.bucketName}' already exists." }
                } else {
                    logger.error(e) { "Error creating MinIO bucket '${minioProperties.bucketName}'." }
                    throw e
                }
            }
        }
        return s3Client
    }
}
