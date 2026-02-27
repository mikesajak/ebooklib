package com.mikesajak.ebooklib.importing.application.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileStoragePort
import com.mikesajak.ebooklib.importing.application.ports.incoming.EbookMetadataExtractorUseCase
import com.mikesajak.ebooklib.importing.application.ports.incoming.UploadToStagingUseCase
import com.mikesajak.ebooklib.importing.application.ports.outgoing.StagedEbookUploadRepositoryPort
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUpload
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadId
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadStatus
import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.springframework.stereotype.Service
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
    private val metadataExtractor: EbookMetadataExtractorUseCase,
    private val repository: StagedEbookUploadRepositoryPort,
    private val objectMapper: ObjectMapper
) : UploadToStagingUseCase {

    override fun upload(fileContent: InputStream, fileName: String, contentType: String): StagedEbookUpload {
        logger.info { "Uploading file to staging: $fileName ($contentType)" }

        // We need to read the stream twice (once for storage, once for parsing) or buffer it.
        // Given we might have large files, buffering in memory might be risky, 
        // but Apache Tika and S3 upload both need the stream.
        // Let's copy the stream to a temporary byte array if it's small, or use a temporary file if it's large.
        // For now, let's assume reasonable sizes and use a byte array for simplicity, 
        // but in a production app we'd use a temporary file.
        
        val fileBytes = fileContent.readAllBytes()
        
        // 1. Extract metadata
        val extracted = try {
            metadataExtractor.extract(ByteArrayInputStream(fileBytes), fileName, contentType)
        } catch (e: Exception) {
            logger.warn(e) { "Failed to extract metadata for $fileName" }
            null
        }

        // 2. Upload ebook to storage
        val ebookMetadata = fileStoragePort.uploadFile(ByteArrayInputStream(fileBytes), fileName, contentType, "staged")
        val uploadId = StagedEbookUploadId(UUID.fromString(ebookMetadata.id))

        // 3. Handle cover if present
        var metadataMap = mutableMapOf<String, Any?>()
        if (extracted != null) {
            metadataMap["title"] = extracted.title
            metadataMap["authors"] = extracted.authors
            metadataMap["creationDate"] = extracted.creationDate?.toString()
            metadataMap["publicationDate"] = extracted.publicationDate?.toString()
            metadataMap["publisher"] = extracted.publisher
            metadataMap["description"] = extracted.description
            
            extracted.coverImage?.let { cover ->
                try {
                    val coverFileMetadata = fileStoragePort.uploadFile(
                        ByteArrayInputStream(cover.data),
                        cover.fileName,
                        cover.contentType,
                        "staged/covers"
                    )
                    metadataMap["coverStorageKey"] = coverFileMetadata.id
                } catch (e: Exception) {
                    logger.warn(e) { "Failed to upload extracted cover for $fileName" }
                }
            }
        }

        val metadataJson = objectMapper.writeValueAsString(metadataMap)

        // 4. Create and save record
        val stagedUpload = StagedEbookUpload(
            id = uploadId,
            fileName = fileName,
            contentType = contentType,
            fileSize = ebookMetadata.size,
            metadataJson = metadataJson,
            status = if (extracted != null) StagedEbookUploadStatus.PARSED else StagedEbookUploadStatus.STAGED,
            createdAt = Instant.now(),
            expiryAt = Instant.now().plus(24, ChronoUnit.HOURS)
        )

        return repository.save(stagedUpload)
    }
}
