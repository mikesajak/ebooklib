package com.mikesajak.ebooklib.importing.infrastructure.adapters.incoming.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.mikesajak.ebooklib.author.domain.model.AuthorId
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.importing.application.ports.incoming.FinalizeImportCommand
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUpload
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadId
import com.mikesajak.ebooklib.importing.infrastructure.adapters.incoming.rest.dto.FinalizeImportRequestDto
import com.mikesajak.ebooklib.importing.infrastructure.adapters.incoming.rest.dto.StagedUploadResponseDto
import com.mikesajak.ebooklib.series.domain.model.SeriesId
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

    fun toCommand(request: FinalizeImportRequestDto): FinalizeImportCommand {
        return FinalizeImportCommand(
            uploadId = StagedEbookUploadId(request.uploadId),
            bookId = request.bookId?.let { BookId(it) },
            title = request.title,
            authorIds = request.authorIds.map { AuthorId(it) },
            publisher = request.publisher,
            publicationDate = request.publicationDate,
            description = request.description,
            seriesId = request.seriesId?.let { SeriesId(it) },
            volume = request.volume,
            labels = request.labels,
            updateCover = request.updateCover
        )
    }
}
