package com.mikesajak.ebooklib.importing.application.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.mikesajak.ebooklib.book.application.ports.incoming.GetBookUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.ListEbookFormatsUseCase
import com.mikesajak.ebooklib.book.application.ports.outgoing.BookRepositoryPort
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.common.domain.model.PaginationRequest
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileStoragePort
import com.mikesajak.ebooklib.importing.application.ports.incoming.EbookMetadataExtractorUseCase
import com.mikesajak.ebooklib.importing.application.ports.incoming.ImportSessionUseCase
import com.mikesajak.ebooklib.importing.application.ports.incoming.MetadataEnrichmentUseCase
import com.mikesajak.ebooklib.importing.application.ports.incoming.ResolutionItemUseCase
import com.mikesajak.ebooklib.importing.application.ports.outgoing.StagedEbookUploadRepositoryPort
import com.mikesajak.ebooklib.importing.domain.model.*
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayInputStream
import java.util.*

private val logger = KotlinLogging.logger {}

@Service
class StagedUploadProcessor(
    private val metadataExtractor: EbookMetadataExtractorUseCase,
    private val getBookUseCase: GetBookUseCase,
    private val listEbookFormatsUseCase: ListEbookFormatsUseCase,
    private val bookRepository: BookRepositoryPort,
    private val repository: StagedEbookUploadRepositoryPort,
    private val fileStoragePort: FileStoragePort,
    private val objectMapper: ObjectMapper,
    private val groupingService: GroupingService,
    private val enrichmentUseCase: MetadataEnrichmentUseCase,
    private val sessionUseCase: ImportSessionUseCase,
    private val resolutionItemUseCase: ResolutionItemUseCase
) {

    @Async
    @Transactional
    fun processAsync(uploadId: StagedEbookUploadId, fileBytes: ByteArray, fileName: String, contentType: String, currentBookId: UUID?) {
        process(uploadId, fileBytes, fileName, contentType, currentBookId)
    }

    @Transactional
    fun process(uploadId: StagedEbookUploadId, fileBytes: ByteArray, fileName: String, contentType: String, currentBookId: UUID?): StagedEbookUpload {
        logger.info { "Processing upload: $uploadId" }
        
        // 1. Extract metadata
        val extracted = try {
            metadataExtractor.extract(ByteArrayInputStream(fileBytes), fileName, contentType)
        } catch (e: Exception) {
            logger.warn(e) { "Failed to extract metadata for $fileName" }
            null
        }

        val metadataMap = mutableMapOf<String, Any?>()
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

            // 4. Perform matching
            val validation = if (currentBookId != null) {
                // Targeted matching
                try {
                    val targetBook = getBookUseCase.getBook(BookId(currentBookId))
                    StagedUploadValidation(candidates = listOf(createCandidate(extracted, targetBook, fileBytes.size.toLong(), fileName)))
                } catch (e: Exception) {
                    logger.warn(e) { "Failed to validate against book $currentBookId" }
                    StagedUploadValidation()
                }
            } else {
                // Automated global matching
                findPotentialMatches(extracted, fileBytes.size.toLong(), fileName)
            }
            metadataMap["validation"] = validation

            // Grouping logic (REQ-003)
            val resolutionItemId = try {
                groupingService.group(uploadId, extracted.title ?: "Untitled", extracted.authors)
            } catch (e: Exception) {
                logger.warn { "Failed to group upload $uploadId: ${e.message}" }
                null
            }

            // External Metadata Enrichment (REQ-004)
            if (extracted.title != null) {
                try {
                    val enrichment = enrichmentUseCase.enrichMetadata(extracted.title, extracted.authors)
                    metadataMap["enrichment"] = enrichment

                    // Also update ResolutionItem if present (REQ-007)
                    if (resolutionItemId != null && enrichment.isNotEmpty()) {
                        resolutionItemUseCase.updateMetadata(resolutionItemId, objectMapper.writeValueAsString(enrichment))
                    }
                } catch (e: Exception) {
                    logger.warn(e) { "Failed to enrich metadata for upload $uploadId" }
                }
            }
        }

        val metadataJson = objectMapper.writeValueAsString(metadataMap)


        val existing = repository.findById(uploadId)
        val result = if (existing != null) {
            val updated = existing.copy(
                metadataJson = metadataJson,
                status = if (extracted != null) StagedEbookUploadStatus.PARSED else StagedEbookUploadStatus.STAGED
            )
            repository.save(updated)
        } else {
            logger.error { "Upload $uploadId not found during processing" }
            throw IllegalStateException("Upload $uploadId not found")
        }

        // Update session progress if applicable
        result.importSessionId?.let { sessionId ->
            try {
                val sessionUploads = repository.findByImportSessionId(sessionId)
                val processed = sessionUploads.count { it.status == StagedEbookUploadStatus.PARSED || it.status == StagedEbookUploadStatus.PROMOTED }
                val failed = sessionUploads.count { it.status == StagedEbookUploadStatus.FAILED }
                sessionUseCase.updateProgress(sessionId, processed, failed)
            } catch (e: Exception) {
                logger.warn { "Failed to update session progress for $sessionId: ${e.message}" }
            }
        }

        return result
    }

    private fun findPotentialMatches(extracted: ExtractedEbookMetadata, fileSize: Long, fileName: String): StagedUploadValidation {
        val title = extracted.title ?: return StagedUploadValidation()
        
        // Search by title (partial/fuzzy via repository)
        val searchResult = bookRepository.findByTitleContaining(title, PaginationRequest(0, 10))
        
        val candidates = searchResult.content.map { book ->
            createCandidate(extracted, book, fileSize, fileName)
        }.sortedByDescending { it.score }

        return StagedUploadValidation(candidates = candidates)
    }

    private fun createCandidate(
        extracted: ExtractedEbookMetadata, 
        book: com.mikesajak.ebooklib.book.domain.model.Book,
        uploadedSize: Long,
        uploadedName: String
    ): MatchCandidate {
        val titleMatch = extracted.title?.let { normalize(it) == normalize(book.title) } ?: false
        
        val extractedAuthorsNormalized = extracted.authors.map { normalize(it) }.toSet()
        val targetAuthorsNormalized = book.authors.map { normalize(it.fullName) }.toSet()
        
        val authorMatch = extractedAuthorsNormalized.isNotEmpty() && targetAuthorsNormalized.isNotEmpty() &&
                extractedAuthorsNormalized == targetAuthorsNormalized

        // Duplicate format check
        val existingFormats = listEbookFormatsUseCase.listFormatFiles(book.id!!)
        val isDuplicate = existingFormats.any { it.fileSize == uploadedSize || it.fileName == uploadedName }

        // Scoring
        var score = 0
        if (titleMatch) score += 80
        if (authorMatch) score += 20
        if (score == 0 && extracted.title != null) score = 50 

        return MatchCandidate(
            bookId = book.id.value,
            title = book.title,
            authors = book.authors.map { it.fullName },
            titleMatch = titleMatch,
            authorMatch = authorMatch,
            duplicateFormat = isDuplicate,
            score = score
        )
    }

    private fun normalize(s: String): String = s.lowercase(Locale.getDefault()).trim()
}
