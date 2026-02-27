package com.mikesajak.ebooklib.importing.infrastructure.adapters.incoming.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUpload
import com.mikesajak.ebooklib.importing.infrastructure.adapters.incoming.rest.dto.StagedUploadResponseDto
import org.springframework.stereotype.Component

@Component
class ImportRestMapper(private val objectMapper: ObjectMapper) {
    fun toResponse(domain: StagedEbookUpload): StagedUploadResponseDto {
        val metadataMap = domain.metadataJson?.let {
            @Suppress("UNCHECKED_CAST")
            objectMapper.readValue(it, Map::class.java) as Map<String, Any?>
        } ?: emptyMap()

        return StagedUploadResponseDto(
            id = domain.id.toString(),
            fileName = domain.fileName,
            contentType = domain.contentType,
            fileSize = domain.fileSize,
            metadata = metadataMap,
            status = domain.status,
            createdAt = domain.createdAt,
            expiryAt = domain.expiryAt
        )
    }
}
