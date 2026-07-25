package com.mikesajak.ebooklib.importing.application.services

import com.mikesajak.ebooklib.file.application.ports.outgoing.FileStoragePort
import com.mikesajak.ebooklib.importing.application.ports.incoming.UploadToStagingUseCase
import com.mikesajak.ebooklib.importing.application.ports.outgoing.StagedEbookUploadRepositoryPort
import com.mikesajak.ebooklib.importing.domain.model.ImportSessionId
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUpload
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadId
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadStatus
import jakarta.transaction.Transactional
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

private val logger = KotlinLogging.logger {}

@Service
@Transactional
class UploadToStagingService(
    private val fileStoragePort: FileStoragePort,
    private val repository: StagedEbookUploadRepositoryPort,
    private val stagedUploadProcessor: StagedUploadProcessor
) : UploadToStagingUseCase {

    override fun upload(fileContent: InputStream, fileName: String, contentType: String, currentBookId: UUID?, importSessionId: UUID?): StagedEbookUpload {
        logger.info { "Uploading file to staging: $fileName ($contentType), currentBookId: $currentBookId, importSessionId: $importSessionId" }

        val fileBytes = fileContent.readAllBytes()
        
        // 1. Upload ebook to storage
        val ebookMetadata = fileStoragePort.uploadFile(ByteArrayInputStream(fileBytes), fileName, contentType, "staged")
        val uploadId = StagedEbookUploadId(UUID.fromString(ebookMetadata.id.substringAfterLast('/')))

        // 2. Create initial record
        val stagedUpload = StagedEbookUpload(
            id = uploadId,
            fileName = fileName,
            contentType = contentType,
            fileSize = ebookMetadata.size,
            metadataJson = null,
            status = StagedEbookUploadStatus.PROCESSING,
            createdAt = Instant.now(),
            expiryAt = Instant.now().plus(24, ChronoUnit.HOURS),
            importSessionId = importSessionId?.let { ImportSessionId(it) }
        )
        repository.save(stagedUpload)

        // 3. Process Sync
        return stagedUploadProcessor.process(uploadId, fileBytes, fileName, contentType, currentBookId)
    }

    override fun uploadAsync(fileContent: InputStream, fileName: String, contentType: String, currentBookId: UUID?, importSessionId: UUID?): StagedEbookUpload {
        logger.info { "Uploading file to staging (ASYNC): $fileName ($contentType), currentBookId: $currentBookId, importSessionId: $importSessionId" }

        val fileBytes = fileContent.readAllBytes()
        
        // 1. Upload ebook to storage
        val ebookMetadata = fileStoragePort.uploadFile(ByteArrayInputStream(fileBytes), fileName, contentType, "staged")
        val uploadId = StagedEbookUploadId(UUID.fromString(ebookMetadata.id.substringAfterLast('/')))

        // 2. Create initial record
        val stagedUpload = StagedEbookUpload(
            id = uploadId,
            fileName = fileName,
            contentType = contentType,
            fileSize = ebookMetadata.size,
            metadataJson = null,
            status = StagedEbookUploadStatus.PROCESSING,
            createdAt = Instant.now(),
            expiryAt = Instant.now().plus(24, ChronoUnit.HOURS),
            importSessionId = importSessionId?.let { ImportSessionId(it) }
        )
        val saved = repository.save(stagedUpload)

        // 3. Process Async after commit (if transaction synchronization is active)
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() {
                    stagedUploadProcessor.processAsync(uploadId, fileBytes, fileName, contentType, currentBookId)
                }
            })
        } else {
            stagedUploadProcessor.processAsync(uploadId, fileBytes, fileName, contentType, currentBookId)
        }
        
        return saved
    }

    override fun retryProcessing(uploadId: UUID) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() {
                    stagedUploadProcessor.retryAsync(StagedEbookUploadId(uploadId))
                }
            })
        } else {
            stagedUploadProcessor.retryAsync(StagedEbookUploadId(uploadId))
        }
    }
}
