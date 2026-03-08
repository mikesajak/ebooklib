package com.mikesajak.ebooklib.file.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.file.application.ports.outgoing.FileEntry
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileMetadata
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileStoragePort
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import java.io.InputStream
import java.util.*

private val logger = KotlinLogging.logger {}

@Component
class S3FileStorageAdapter(
        private val s3Client: S3Client,
        @Value("\${minio.bucket-name:ebook-library-files}") private val bucketName: String
) : FileStoragePort {
    override fun uploadFile(fileContent: InputStream, originalFileName: String, contentType: String, folder: String?): FileMetadata {
        val fileId = UUID.randomUUID().toString()
        val storageKey = if (folder != null) "$folder/$fileId" else fileId

        val putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(storageKey)
                .contentType(contentType)
                .build()

        fileContent.use {
            val response =
                s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(it, it.available().toLong()))
            logger.info { "File uploaded to S3: key=$storageKey, ETag=${response.eTag()}" }
        }

        // Get actual size after upload if needed, or rely on available()
        val headObjectRequest = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(storageKey)
                .build()
        val headResponse = s3Client.headObject(headObjectRequest)
        val fileSize = headResponse.contentLength()

        return FileMetadata(storageKey, originalFileName, contentType, fileSize)
    }

    override fun downloadFile(fileId: String): InputStream {
        val getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileId)
                .build()
        return s3Client.getObject(getObjectRequest)
    }

    override fun deleteFile(fileId: String) {
        val deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(fileId)
                .build()
        s3Client.deleteObject(deleteObjectRequest)
        logger.info { "File deleted from S3: key=$fileId" }
    }

    override fun getFileMetadata(fileId: String): FileMetadata? {
        return try {
            val headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileId)
                    .build()
            val headResponse = s3Client.headObject(headObjectRequest)
            // Note: originalFileName is not stored in S3 metadata by default,
            // so we might need to store it in our DB or pass it around.
            // For now, using fileId as a placeholder for fileName if not available.
            val fileName = fileId.substringAfterLast('/')
            FileMetadata(fileId, fileName, headResponse.contentType(), headResponse.contentLength())
        } catch (e: NoSuchKeyException) {
            logger.warn { "File metadata not found for key: $fileId" }
            null
        } catch (e: Exception) {
            logger.error(e) { "Error getting file metadata for key: $fileId" }
            null
        }
    }

    override fun moveFile(fileId: String, newFolder: String?): FileMetadata {
        val fileName = fileId.substringAfterLast('/')
        val newStorageKey = if (newFolder != null) "$newFolder/$fileName" else fileName

        logger.info { "Moving file in S3 from $fileId to $newStorageKey" }

        val copyObjectRequest = CopyObjectRequest.builder()
            .sourceBucket(bucketName)
            .sourceKey(fileId)
            .destinationBucket(bucketName)
            .destinationKey(newStorageKey)
            .build()

        s3Client.copyObject(copyObjectRequest)
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(fileId).build())

        val headResponse = s3Client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(newStorageKey).build())
        return FileMetadata(newStorageKey, fileName, headResponse.contentType(), headResponse.contentLength())
    }

    override fun listAllFiles(prefix: String?): Sequence<FileEntry> {
        val listRequest = ListObjectsV2Request.builder()
            .bucket(bucketName)
            .let { if (prefix != null) it.prefix(prefix) else it }
            .build()

        return s3Client.listObjectsV2Paginator(listRequest)
            .contents()
            .asSequence()
            .map { FileEntry(it.key(), it.size()) }
    }
}
