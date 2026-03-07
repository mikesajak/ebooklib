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
import mu.KotlinLogging
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
        val extracted = parseExtracted(metadataMap["metadata"])

        val bestCandidate = validation?.candidates?.find { it.score >= 80 }
        val matchBookId = bestCandidate?.bookId?.let { BookId(it) }

        // Strategy Logic
        val command = when (strategy) {
            AutoResolveStrategy.NEW_ONLY -> {
                if (matchBookId != null) {
                    logger.info { "Item ${item.id} matched with book $matchBookId. Skipping NEW_ONLY strategy." }
                    return
                }
                buildIncomingCommand(primaryUpload.id, null, extracted, enrichment)
            }
            AutoResolveStrategy.TRUST_INCOMING -> {
                buildIncomingCommand(primaryUpload.id, matchBookId, extracted, enrichment)
            }
            AutoResolveStrategy.TRUST_EXISTING -> {
                if (matchBookId != null) {
                    buildExistingCommand(primaryUpload.id, matchBookId)
                } else {
                    // Fallback to incoming if no match? Or skip? 
                    // Let's fallback to incoming so we don't leave things unresolved if they are new.
                    buildIncomingCommand(primaryUpload.id, null, extracted, enrichment)
                }
            }
        }

        finalizeImportUseCase.finalize(command)
        logger.info { "Auto-resolved item ${item.id} successfully" }
    }

    private fun buildIncomingCommand(
        uploadId: StagedEbookUploadId, 
        bookId: BookId?, 
        extracted: ExtractedEbookMetadata?, 
        enrichment: List<EnrichedMetadata>?
    ): FinalizeImportCommand {
        val external = enrichment?.firstOrNull()
        
        return FinalizeImportCommand(
            uploadId = uploadId,
            bookId = bookId,
            title = external?.title ?: extracted?.title ?: "Untitled",
            authorIds = emptyList(), // We'll use authorNames for auto-resolve to avoid manual ID matching
            authorNames = external?.authors ?: extracted?.authors ?: emptyList(),
            publisher = external?.publisher ?: extracted?.publisher,
            publicationDate = external?.publicationDate ?: extracted?.publicationDate,
            description = external?.description ?: extracted?.description,
            seriesId = null, // Auto-linking series by name is risky, leave for manual if needed
            volume = external?.volume,
            updateCover = external?.coverUrl != null || extracted?.coverImage != null
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
