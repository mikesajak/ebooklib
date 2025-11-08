package com.mikesajak.ebooklib.config

import org.mockito.Mockito
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ListBucketsResponse
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.CreateBucketResponse

@TestConfiguration
class TestMinioConfig {

    @Bean
    @Primary // This bean will take precedence over the one in MinioConfig
    fun mockS3Client(): S3Client {
        val mockS3Client = Mockito.mock(S3Client::class.java)

        // Stub listBuckets to return an empty list of buckets
        Mockito.`when`(mockS3Client.listBuckets()).thenReturn(ListBucketsResponse.builder().buckets(emptyList()).build())

        // Stub createBucket to do nothing
        Mockito.`when`(mockS3Client.createBucket(Mockito.any(CreateBucketRequest::class.java)))
            .thenReturn(CreateBucketResponse.builder().build())

        return mockS3Client
    }
}
