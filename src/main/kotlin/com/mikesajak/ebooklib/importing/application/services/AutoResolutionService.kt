package com.mikesajak.ebooklib.importing.application.services

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.mikesajak.ebooklib.author.domain.model.AuthorId
import com.mikesajak.ebooklib.book.application.ports.incoming.GetBookUseCase
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.importing.application.ports.incoming.*
import com.mikesajak.ebooklib.importing.application.ports.outgoing.StagedEbookUploadRepositoryPort
import com.mikesajak.ebooklib.importing.domain.model.*
import com.mikesajak.ebooklib.series.domain.model.SeriesId
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

private val logger = KotlinLogging.logger {}

@Service
class AutoResolutionService(
    private val resolutionItemUseCase: ResolutionItemUseCase,
    private val stagedUploadRepository: StagedEbookUploadRepositoryPort,
    private val finalizeImportUseCase: FinalizeImportUseCase,
    private val getBookUseCase: GetBookUseCase,
    private val objectMapper: ObjectMapper
) : AutoResolutionUseCase {

    @Transactional
    override fun autoResolve(sessionId: ImportSessionId, itemIds: List<ResolutionItemId>?, strategy: AutoResolveStrategy) {
        logger.info { "Auto-resolving items for session $sessionId using strategy $strategy. Item filter: ${itemIds ?: "ALL"}" }

        val allItems = resolutionItemUseCase.getResolutionItems(sessionId)
        val targetItems = allItems.filter { item ->
            item.status == ResolutionItemStatus.UNRESOLVED && (itemIds == null || itemIds.contains(item.id))
        }

        logger.info { "Found ${targetItems.size} unresolved items to auto-resolve" }

        targetItems.forEach { item ->
            try {
                autoResolveItem(item, strategy)
            } catch (e: Exception) {
                logger.error(e) { "Failed to auto-resolve item ${item.id} (${item.title})" }
                // Continue with other items
            }
        }
    }

    private fun autoResolveItem(item: ResolutionItem, strategy: AutoResolveStrategy) {
        val formats = stagedUploadRepository.findByResolutionItemId(item.id.value)
        if (formats.isEmpty()) {
            logger.warn { "No formats found for resolution item ${item.id}. Skipping." }
            return
        }

        // Use the first format as primary for metadata
        val primaryUpload = formats.first()
        val metadataMap = primaryUpload.metadataJson?.let {
            try {
                objectMapper.readValue(it, object : TypeReference<Map<String, Any?>>() {})
            } catch (e: Exception) {
                null
            }
        } ?: emptyMap()

        val validation = parseValidation(metadataMap["validation"])
        val enrichment = parseEnrichment(metadataMap["enrichment"])

        val rawTitle = (metadataMap["title"] as? String) ?: item.title
        val fileNameTitle = (metadataMap["fileNameTitle"] as? String) ?: ImportUtils.extractTitleFromFileName(primaryUpload.fileName)
        val title = if (!rawTitle.isNullOrBlank() && !ImportUtils.isUnlikelyTitle(rawTitle)) rawTitle else fileNameTitle
        @Suppress("UNCHECKED_CAST")
        val authors = (metadataMap["authors"] as? List<String>) ?: item.authors
        val publisher = metadataMap["publisher"] as? String
        val publicationDateStr = metadataMap["publicationDate"] as? String
        val publicationDate = publicationDateStr?.let {
            try { java.time.LocalDate.parse(it) } catch (e: Exception) { null }
        }
        val description = metadataMap["description"] as? String

        val hasExtractedCover = formats.any { upload ->
            val map = upload.metadataJson?.let {
                try {
                    objectMapper.readValue(it, object : TypeReference<Map<String, Any?>>() {})
                } catch (e: Exception) { null }
            }
            map?.get("coverStorageKey") != null
        }

        val bestCandidate = validation?.candidates?.find { it.score >= 80 }
        val matchBookId = bestCandidate?.bookId?.let { BookId(it) }

        // Strategy Logic
        val command = when (strategy) {
            AutoResolveStrategy.NEW_ONLY -> {
                if (matchBookId != null) {
                    logger.info { "Item ${item.id} matched with book $matchBookId. Skipping NEW_ONLY strategy." }
                    return
                }
                buildIncomingCommand(primaryUpload.id, null, title, authors, publisher, publicationDate, description, enrichment, hasExtractedCover)
            }
            AutoResolveStrategy.TRUST_INCOMING -> {
                buildIncomingCommand(primaryUpload.id, matchBookId, title, authors, publisher, publicationDate, description, enrichment, hasExtractedCover)
            }
            AutoResolveStrategy.TRUST_EXISTING -> {
                if (matchBookId != null) {
                    buildExistingCommand(primaryUpload.id, matchBookId)
                } else {
                    buildIncomingCommand(primaryUpload.id, null, title, authors, publisher, publicationDate, description, enrichment, hasExtractedCover)
                }
            }
        }

        finalizeImportUseCase.finalize(command)
        logger.info { "Auto-resolved item ${item.id} successfully" }
    }

    private fun buildIncomingCommand(
        uploadId: StagedEbookUploadId, 
        bookId: BookId?, 
        title: String,
        authors: List<String>,
        publisher: String?,
        publicationDate: java.time.LocalDate?,
        description: String?,
        enrichment: List<EnrichedMetadata>?,
        hasExtractedCover: Boolean
    ): FinalizeImportCommand {
        val external = enrichment?.firstOrNull()
        
        return FinalizeImportCommand(
            uploadId = uploadId,
            bookId = bookId,
            title = external?.title ?: title,
            authorIds = emptyList(), // We'll use authorNames for auto-resolve to avoid manual ID matching
            authorNames = external?.authors ?: authors,
            publisher = external?.publisher ?: publisher,
            publicationDate = external?.publicationDate ?: publicationDate,
            description = external?.description ?: description,
            seriesId = null, // Auto-linking series by name is risky, leave for manual if needed
            volume = external?.volume,
            updateCover = external?.coverUrl != null || hasExtractedCover
        )
    }

    private fun buildExistingCommand(uploadId: StagedEbookUploadId, bookId: BookId): FinalizeImportCommand {
        val existingBook = getBookUseCase.getBook(bookId)
        
        return FinalizeImportCommand(
            uploadId = uploadId,
            bookId = bookId,
            title = existingBook.title,
            authorIds = existingBook.authors.map { it.id }.filterNotNull(),
            authorNames = emptyList(),
            publisher = existingBook.publisher,
            publicationDate = existingBook.publicationDate,
            description = existingBook.description,
            seriesId = existingBook.series?.id,
            volume = existingBook.volume,
            labels = existingBook.labels,
            updateCover = false // Trust existing cover
        )
    }

    private fun parseValidation(validationObj: Any?): StagedUploadValidation? {
        if (validationObj == null) return null
        return try {
            objectMapper.convertValue(validationObj, StagedUploadValidation::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseEnrichment(enrichmentObj: Any?): List<EnrichedMetadata>? {
        if (enrichmentObj == null) return null
        return try {
            objectMapper.convertValue(enrichmentObj, object : TypeReference<List<EnrichedMetadata>>() {})
        } catch (e: Exception) {
            null
        }
    }

    private fun parseExtracted(extractedObj: Any?): ExtractedEbookMetadata? {
        if (extractedObj == null) return null
        return try {
            objectMapper.convertValue(extractedObj, ExtractedEbookMetadata::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
