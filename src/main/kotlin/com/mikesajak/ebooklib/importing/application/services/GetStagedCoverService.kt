package com.mikesajak.ebooklib.importing.application.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileStoragePort
import com.mikesajak.ebooklib.importing.application.ports.incoming.GetStagedCoverUseCase
import com.mikesajak.ebooklib.importing.application.ports.incoming.StagedCover
import com.mikesajak.ebooklib.importing.application.ports.outgoing.StagedEbookUploadRepositoryPort
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadId
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import jakarta.transaction.Transactional

private val logger = KotlinLogging.logger {}

@Service
@Transactional
class GetStagedCoverService(
    private val repository: StagedEbookUploadRepositoryPort,
    private val fileStoragePort: FileStoragePort,
    private val objectMapper: ObjectMapper
) : GetStagedCoverUseCase {

    override fun getCover(uploadId: StagedEbookUploadId): StagedCover? {
        val stagedUpload = repository.findById(uploadId) ?: return null

        val metadataMap = stagedUpload.metadataJson?.let {
            @Suppress("UNCHECKED_CAST")
            objectMapper.readValue(it, Map::class.java) as Map<String, Any?>
        } ?: return null

        val coverStorageKey = metadataMap["coverStorageKey"] as? String ?: return null

        return try {
            val fileMetadata = fileStoragePort.getFileMetadata(coverStorageKey) ?: return null
            val inputStream = fileStoragePort.downloadFile(coverStorageKey)
            StagedCover(inputStream, fileMetadata.contentType)
        } catch (e: Exception) {
            logger.warn(e) { "Failed to download staged cover $coverStorageKey for upload $uploadId" }
            null
        }
    }
}
