package com.mikesajak.ebooklib.infrastructure.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.net.URI

@Configuration
class MinioConfig(
    @Value("\${minio.endpoint}") private val minioEndpoint: String,
    @Value("\${minio.access-key}") private val minioAccessKey: String,
    @Value("\${minio.secret-key}") private val minioSecretKey: String
) {

    @Bean
    fun s3Client(): S3Client {
        return S3Client.builder()
            .endpointOverride(URI.create(minioEndpoint))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(minioAccessKey, minioSecretKey)
                )
            )
            .region(Region.US_EAST_1) // MinIO doesn't strictly use regions, but a default is required
            .build()
    }
}
