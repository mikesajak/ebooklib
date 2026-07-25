package com.mikesajak.ebooklib.importing.application.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.mikesajak.ebooklib.author.application.ports.incoming.GetAuthorUseCase
import com.mikesajak.ebooklib.author.application.ports.incoming.SaveAuthorUseCase
import com.mikesajak.ebooklib.author.application.ports.outgoing.AuthorRepositoryPort
import com.mikesajak.ebooklib.author.domain.model.Author
import com.mikesajak.ebooklib.book.application.ports.incoming.AddBookUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.AddEbookFormatUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.GetBookUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.UpdateBookUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.UploadBookCoverUseCase
import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileStoragePort
import com.mikesajak.ebooklib.importing.application.ports.incoming.FinalizeImportCommand
import com.mikesajak.ebooklib.importing.application.ports.incoming.FinalizeImportUseCase
import com.mikesajak.ebooklib.importing.application.ports.incoming.ResolutionItemUseCase
import com.mikesajak.ebooklib.importing.application.ports.outgoing.StagedEbookUploadRepositoryPort
import com.mikesajak.ebooklib.importing.domain.model.ResolutionItemId
import com.mikesajak.ebooklib.importing.domain.model.ResolutionItemStatus
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUpload
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadId
import com.mikesajak.ebooklib.series.application.ports.incoming.GetSeriesUseCase
import jakarta.transaction.Transactional
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
@Transactional
class FinalizeImportService(
    private val stagedRepository: StagedEbookUploadRepositoryPort,
    private val fileStoragePort: FileStoragePort,
    private val getBookUseCase: GetBookUseCase,
    private val addBookUseCase: AddBookUseCase,
    private val updateBookUseCase: UpdateBookUseCase,
    private val getAuthorUseCase: GetAuthorUseCase,
    private val saveAuthorUseCase: SaveAuthorUseCase,
    private val authorRepository: AuthorRepositoryPort,
    private val getSeriesUseCase: GetSeriesUseCase,
    private val addEbookFormatUseCase: AddEbookFormatUseCase,
    private val uploadBookCoverUseCase: UploadBookCoverUseCase,
    private val resolutionItemUseCase: ResolutionItemUseCase,
    private val objectMapper: ObjectMapper
) : FinalizeImportUseCase {

    override fun finalize(command: FinalizeImportCommand): Book {
        logger.info { "Finalizing import for uploadId: ${command.uploadId}, bookId: ${command.bookId}, skipFormat: ${command.skipFormatLink}" }

        val mainStagedUpload = stagedRepository.findById(command.uploadId)
            ?: throw IllegalArgumentException("Staged upload not found: ${command.uploadId}")

        // 1. Resolve all authors (existing + new)
        val allAuthors = resolveAuthors(command)

        // 2. Create or Update Book
        val book = if (command.bookId != null) {
            updateExistingBook(command, allAuthors)
        } else {
            createNewBook(command, allAuthors)
        }

        // 3. Promote ALL associated formats (if grouped)
        val allUploads = if (mainStagedUpload.resolutionItemId != null) {
            stagedRepository.findByResolutionItemId(mainStagedUpload.resolutionItemId)
        } else {
            listOf(mainStagedUpload)
        }

        logger.info { "Found ${allUploads.size} formats to promote for resolution item ${mainStagedUpload.resolutionItemId}" }

        allUploads.forEach { stagedUpload ->
            promoteFormat(stagedUpload, book, command.skipFormatLink)
        }

        // 4. Handle Cover Promotion
        if (command.updateCover) {
            promoteCover(mainStagedUpload, allUploads, book)
        }

        // 5. Update Resolution Item metadata and status
        mainStagedUpload.resolutionItemId?.let { resItemId ->
            val authorNamesList = if (command.authorNames.isNotEmpty()) {
                command.authorNames
            } else {
                allAuthors.map { "${it.firstName} ${it.lastName}".trim() }
            }

            val resolvedDataMap = mapOf(
                "bookId" to book.id?.value?.toString(),
                "title" to command.title,
                "authorIds" to command.authorIds.map { it.value.toString() },
                "authorNames" to command.authorNames,
                "publisher" to command.publisher,
                "publicationDate" to command.publicationDate,
                "description" to command.description,
                "seriesId" to command.seriesId?.value?.toString(),
                "volume" to command.volume,
                "labels" to command.labels,
                "updateCover" to command.updateCover,
                "skipFormatLink" to command.skipFormatLink
            )
            val json = objectMapper.writeValueAsString(resolvedDataMap)

            resolutionItemUseCase.updateResolvedItem(
                ResolutionItemId(resItemId),
                title = command.title,
                authors = authorNamesList,
                status = ResolutionItemStatus.RESOLVED,
                metadataJson = json
            )
        }

        return getBookUseCase.getBook(book.id!!)
    }

    private fun promoteFormat(stagedUpload: StagedEbookUpload, book: Book, skipFormatLink: Boolean) {
        if (skipFormatLink) {
            logger.info { "Skipping format linking for upload ${stagedUpload.id} as requested" }
            return
        }

        try {
            val sourceKey = "staged/${stagedUpload.id}"
            logger.info { "Promoting format for upload ${stagedUpload.id} to book ${book.id}. Source key: $sourceKey" }
            
            // Check if file still in staged (it might have been moved in a previous failed attempt)
            val stagedMetadata = fileStoragePort.getFileMetadata(sourceKey)
            
            val promotedFileId = if (stagedMetadata != null) {
                val moved = fileStoragePort.moveFile(sourceKey, null)
                logger.info { "Moved staged file $sourceKey to permanent storage: ${moved.id}" }
                moved.id
            } else {
                // If not in staged, check if it's already in permanent storage (root)
                val rootKey = stagedUpload.id.toString()
                if (fileStoragePort.getFileMetadata(rootKey) != null) {
                    logger.info { "File ${stagedUpload.id} already in permanent storage at $rootKey" }
                    rootKey
                } else {
                    logger.warn { "Staged file ${stagedUpload.id} not found in staged/ or root. Skipping promotion." }
                    null
                }
            }

            if (promotedFileId != null) {
                val formatType = extractFormatType(stagedUpload.fileName)
                logger.info { "Linking promoted file $promotedFileId as format $formatType to book ${book.id}" }
                addEbookFormatUseCase.addFormatFromStorage(book.id!!, promotedFileId, formatType, stagedUpload.fileName)
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to promote format ${stagedUpload.id} for book ${book.id}" }
            // We continue with other formats even if one fails
        }
    }

    private fun promoteCover(mainStagedUpload: StagedEbookUpload, allUploads: List<StagedEbookUpload>, book: Book) {
        // Search mainStagedUpload first, then all sibling uploads in the resolution item for a cover
        val uploadsToCheck = listOf(mainStagedUpload) + (allUploads - mainStagedUpload)

        var coverStorageKey: String? = null
        for (upload in uploadsToCheck) {
            val metadataMap = upload.metadataJson?.let {
                @Suppress("UNCHECKED_CAST")
                try {
                    objectMapper.readValue(it, Map::class.java) as Map<String, Any?>
                } catch (e: Exception) {
                    null
                }
            }
            val key = metadataMap?.get("coverStorageKey") as? String
            if (key != null) {
                coverStorageKey = key
                break
            }
        }

        if (coverStorageKey == null) return

        try {
            if (fileStoragePort.getFileMetadata(coverStorageKey) != null) {
                val promotedCover = fileStoragePort.moveFile(coverStorageKey, "covers")
                uploadBookCoverUseCase.setCoverFromStorage(book.id!!, promotedCover.id)
            } else {
                // Check if already promoted
                val fileName = coverStorageKey.substringAfterLast('/')
                val rootKey = "covers/$fileName"
                if (fileStoragePort.getFileMetadata(rootKey) != null) {
                    uploadBookCoverUseCase.setCoverFromStorage(book.id!!, rootKey)
                } else {
                    logger.warn { "Staged cover $coverStorageKey not found. Skipping cover promotion." }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to promote cover $coverStorageKey for book ${book.id}" }
        }
    }

    private fun resolveAuthors(command: FinalizeImportCommand): List<Author> {
        val existingAuthorsByDirectId = command.authorIds.map { getAuthorUseCase.getAuthor(it) }
        
        val authorsFromNames = command.authorNames.map { name ->
            val (firstName, lastName) = splitAuthorName(name)
            
            val existingAuthor = authorRepository.findByName(firstName, lastName)
            if (existingAuthor != null) {
                logger.info { "Matched existing author by name: $name" }
                existingAuthor
            } else {
                logger.info { "Creating new author from import: $name" }
                saveAuthorUseCase.saveAuthor(Author(null, firstName, lastName, null, null, null))
            }
        }
        
        return (existingAuthorsByDirectId + authorsFromNames).distinctBy { it.id }
    }

    private fun splitAuthorName(fullName: String): Pair<String, String> {
        val parts = fullName.trim().split(" ")
        return if (parts.size > 1) {
            Pair(parts.first(), parts.drop(1).joinToString(" "))
        } else {
            Pair("", fullName)
        }
    }

    private fun createNewBook(command: FinalizeImportCommand, authors: List<Author>): Book {
        val series = command.seriesId?.let { getSeriesUseCase.getSeries(it) }
        
        val newBook = Book(
            id = null,
            title = command.title,
            authors = authors,
            creationDate = null,
            publicationDate = command.publicationDate,
            publisher = command.publisher,
            description = command.description,
            series = series,
            volume = command.volume,
            labels = command.labels
        )
        return addBookUseCase.addBook(newBook)
    }

    private fun updateExistingBook(command: FinalizeImportCommand, authors: List<Author>): Book {
        val existingBook = getBookUseCase.getBook(command.bookId!!)
        val series = command.seriesId?.let { getSeriesUseCase.getSeries(it) }

        val updatedBook = existingBook.copy(
            title = command.title,
            authors = authors,
            publicationDate = command.publicationDate ?: existingBook.publicationDate,
            publisher = command.publisher ?: existingBook.publisher,
            description = command.description ?: existingBook.description,
            series = series ?: existingBook.series,
            volume = command.volume ?: existingBook.volume,
            labels = if (command.labels.isNotEmpty()) command.labels else existingBook.labels
        )
        return updateBookUseCase.updateBook(updatedBook)
    }

    private fun extractFormatType(fileName: String): String {
        return fileName.substringAfterLast('.').uppercase()
    }
}
