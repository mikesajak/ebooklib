package com.mikesajak.ebooklib.importing.application.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.mikesajak.ebooklib.book.application.ports.incoming.GetBookUseCase
import com.mikesajak.ebooklib.book.application.ports.outgoing.BookRepositoryPort
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.common.domain.model.PaginationRequest
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileStoragePort
import com.mikesajak.ebooklib.importing.application.ports.incoming.EbookMetadataExtractorUseCase
import com.mikesajak.ebooklib.importing.application.ports.incoming.UploadToStagingUseCase
import com.mikesajak.ebooklib.importing.application.ports.outgoing.StagedEbookUploadRepositoryPort
import com.mikesajak.ebooklib.importing.domain.model.*
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
    private val getBookUseCase: GetBookUseCase,
    private val bookRepository: BookRepositoryPort,
    private val repository: StagedEbookUploadRepositoryPort,
    private val objectMapper: ObjectMapper
) : UploadToStagingUseCase {

    override fun upload(fileContent: InputStream, fileName: String, contentType: String, currentBookId: UUID?): StagedEbookUpload {
        logger.info { "Uploading file to staging: $fileName ($contentType), currentBookId: $currentBookId" }

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
        val uploadId = StagedEbookUploadId(UUID.fromString(ebookMetadata.id.substringAfterLast('/')))

        // 3. Handle cover if present
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
                    StagedUploadValidation(candidates = listOf(createCandidate(extracted, targetBook)))
                } catch (e: Exception) {
                    logger.warn(e) { "Failed to validate against book $currentBookId" }
                    StagedUploadValidation()
                }
            } else {
                // Automated global matching
                findPotentialMatches(extracted)
            }
            metadataMap["validation"] = validation
        }

        val metadataJson = objectMapper.writeValueAsString(metadataMap)

        // 5. Create and save record
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

    private fun findPotentialMatches(extracted: ExtractedEbookMetadata): StagedUploadValidation {
        val title = extracted.title ?: return StagedUploadValidation()
        
        // Search by title (partial/fuzzy via repository)
        val searchResult = bookRepository.findByTitleContaining(title, PaginationRequest(0, 10))
        
        val candidates = searchResult.content.map { book ->
            createCandidate(extracted, book)
        }.sortedByDescending { it.score }

        return StagedUploadValidation(candidates = candidates)
    }

    private fun createCandidate(extracted: ExtractedEbookMetadata, book: com.mikesajak.ebooklib.book.domain.model.Book): MatchCandidate {
        val titleMatch = extracted.title?.let { normalize(it) == normalize(book.title) } ?: false
        
        val extractedAuthorsNormalized = extracted.authors.map { normalize(it) }.toSet()
        val targetAuthorsNormalized = book.authors.map { normalize(it.fullName) }.toSet()
        
        val authorMatch = extractedAuthorsNormalized.isNotEmpty() && targetAuthorsNormalized.isNotEmpty() &&
                extractedAuthorsNormalized == targetAuthorsNormalized

        // Scoring: 
        // 100 for exact title + author
        // 80 for exact title
        // 50 for partial title (which is what we get from repo)
        var score = 0
        if (titleMatch) score += 80
        if (authorMatch) score += 20
        if (score == 0 && extracted.title != null) score = 50 

        return MatchCandidate(
            bookId = book.id!!.value,
            title = book.title,
            authors = book.authors.map { it.fullName },
            titleMatch = titleMatch,
            authorMatch = authorMatch,
            score = score
        )
    }

    private fun normalize(s: String): String = s.lowercase(Locale.getDefault()).trim()
}
