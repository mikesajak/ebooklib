package com.mikesajak.ebooklib.importing.infrastructure.adapters.incoming.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.mikesajak.ebooklib.author.domain.model.AuthorId
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.importing.application.ports.incoming.FinalizeImportCommand
import com.mikesajak.ebooklib.importing.domain.model.*
import com.mikesajak.ebooklib.importing.infrastructure.adapters.incoming.rest.dto.*
import com.mikesajak.ebooklib.series.domain.model.SeriesId
import org.springframework.stereotype.Component
import java.util.*

@Component
class ImportRestMapper(private val objectMapper: ObjectMapper) {
    fun toResponse(domain: StagedEbookUpload): StagedUploadResponseDto {
        val metadataMap = try {
            domain.metadataJson?.let {
                @Suppress("UNCHECKED_CAST")
                objectMapper.readValue(it, Map::class.java) as Map<String, Any?>
            } ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }

        // Extract validation from metadataMap if present (stored as JSON in domain)
        val validation = metadataMap["validation"]?.let { valObj ->
            try {
                @Suppress("UNCHECKED_CAST")
                val valMap = valObj as? Map<String, Any?>
                @Suppress("UNCHECKED_CAST")
                val candidatesList = (valMap?.get("candidates") as? List<Map<String, Any?>>) ?: emptyList()

                StagedUploadValidationDto(
                    candidates = candidatesList.mapNotNull { c ->
                        try {
                            val bookIdStr = c["bookId"]?.toString() ?: return@mapNotNull null
                            @Suppress("UNCHECKED_CAST")
                            MatchCandidateDto(
                                bookId = UUID.fromString(bookIdStr),
                                title = c["title"]?.toString() ?: "Unknown",
                                authors = (c["authors"] as? List<String>) ?: emptyList(),
                                titleMatch = (c["titleMatch"] as? Boolean) ?: false,
                                authorMatch = (c["authorMatch"] as? Boolean) ?: false,
                                duplicateFormat = (c["duplicateFormat"] as? Boolean) ?: false,
                                score = (c["score"] as? Number)?.toInt() ?: 0
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                )
            } catch (e: Exception) {
                null
            }
        }

        return StagedUploadResponseDto(
            id = domain.id.toString(),
            fileName = domain.fileName,
            contentType = domain.contentType,
            fileSize = domain.fileSize,
            metadata = metadataMap,
            validation = validation,
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
            authorNames = request.authorNames,
            publisher = request.publisher,
            publicationDate = request.publicationDate,
            description = request.description,
            seriesId = request.seriesId?.let { SeriesId(it) },
            volume = request.volume,
            labels = request.labels,
            updateCover = request.updateCover,
            skipFormatLink = request.skipFormatLink
        )
    }

    fun toResponse(session: ImportSession): ImportSessionResponseDto {
        return ImportSessionResponseDto(
            id = session.id.toString(),
            status = session.status,
            totalFiles = session.totalFiles,
            processedFiles = session.processedFiles,
            failedFiles = session.failedFiles,
            createdAt = session.createdAt,
            updatedAt = session.updatedAt,
            expiryAt = session.expiryAt
        )
    }

    fun toResponse(item: ResolutionItem, formats: List<StagedEbookUpload>): ResolutionItemResponseDto {
        val metadata = parseMetadata(item.metadataJson)
        return ResolutionItemResponseDto(
            id = item.id.toString(),
            importSessionId = item.importSessionId.toString(),
            title = item.title,
            authors = item.authors,
            status = item.status,
            createdAt = item.createdAt,
            updatedAt = item.updatedAt,
            metadataJson = item.metadataJson,
            metadata = metadata,
            formats = formats.map { f ->
                ResolutionItemFormatDto(
                    uploadId = f.id.toString(),
                    fileName = f.fileName,
                    contentType = f.contentType,
                    fileSize = f.fileSize
                )
            }
        )
    }

    fun toResolutionItemResponse(upload: StagedEbookUpload): ResolutionItemResponseDto {
        val metadata = parseMetadata(upload.metadataJson)
        return ResolutionItemResponseDto(
            id = upload.id.toString(),
            importSessionId = upload.importSessionId?.toString() ?: "",
            title = upload.fileName,
            authors = emptyList(),
            status = when (upload.status) {
                StagedEbookUploadStatus.PROCESSING -> ResolutionItemStatus.PROCESSING
                StagedEbookUploadStatus.STAGED -> ResolutionItemStatus.STAGED
                StagedEbookUploadStatus.FAILED -> ResolutionItemStatus.ERROR
                else -> ResolutionItemStatus.UNRESOLVED
            },
            createdAt = upload.createdAt,
            updatedAt = upload.createdAt,
            metadataJson = upload.metadataJson,
            metadata = metadata,
            formats = listOf(
                ResolutionItemFormatDto(
                    uploadId = upload.id.toString(),
                    fileName = upload.fileName,
                    contentType = upload.contentType,
                    fileSize = upload.fileSize
                )
            )
        )
    }

    private fun parseMetadata(json: String?): Map<String, Any?> {
        return try {
            json?.let {
                @Suppress("UNCHECKED_CAST")
                objectMapper.readValue(it, Map::class.java) as Map<String, Any?>
            } ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
