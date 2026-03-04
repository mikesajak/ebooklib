package com.mikesajak.ebooklib.importing.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUpload
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadId
import org.springframework.stereotype.Component

@Component
class StagedEbookUploadEntityMapper {
    fun toEntity(domain: StagedEbookUpload): StagedEbookUploadEntity =
        StagedEbookUploadEntity(
            id = domain.id.value,
            fileName = domain.fileName,
            contentType = domain.contentType,
            fileSize = domain.fileSize,
            metadataJson = domain.metadataJson,
            status = domain.status,
            createdAt = domain.createdAt,
            expiryAt = domain.expiryAt,
            importSessionId = domain.importSessionId?.value,
            resolutionItemId = domain.resolutionItemId
        )

    fun toDomain(entity: StagedEbookUploadEntity): StagedEbookUpload =
        StagedEbookUpload(
            id = StagedEbookUploadId(entity.id),
            fileName = entity.fileName,
            contentType = entity.contentType,
            fileSize = entity.fileSize,
            metadataJson = entity.metadataJson,
            status = entity.status,
            createdAt = entity.createdAt,
            expiryAt = entity.expiryAt,
            importSessionId = entity.importSessionId?.let { ImportSessionId(it) },
            resolutionItemId = entity.resolutionItemId
        )
}
