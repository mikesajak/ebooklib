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
import com.mikesajak.ebooklib.importing.application.ports.outgoing.StagedEbookUploadRepositoryPort
import com.mikesajak.ebooklib.series.application.ports.incoming.GetSeriesUseCase
import jakarta.transaction.Transactional
import mu.KotlinLogging
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
    private val objectMapper: ObjectMapper
) : FinalizeImportUseCase {

    override fun finalize(command: FinalizeImportCommand): Book {
        logger.info { "Finalizing import for uploadId: ${command.uploadId}, bookId: ${command.bookId}" }

        val stagedUpload = stagedRepository.findById(command.uploadId)
            ?: throw IllegalArgumentException("Staged upload not found: ${command.uploadId}")

        // 1. Resolve all authors (existing + new)
        val allAuthors = resolveAuthors(command)

        // 2. Create or Update Book
        val book = if (command.bookId != null) {
            updateExistingBook(command, allAuthors)
        } else {
            createNewBook(command, allAuthors)
        }

        // 3. Promote Ebook File
        val promotedFile = fileStoragePort.moveFile("staged/${stagedUpload.id}", null) 
        
        // 4. Link Format to Book
        val formatType = extractFormatType(stagedUpload.fileName)
        addEbookFormatUseCase.addFormatFromStorage(book.id!!, promotedFile.id, formatType)

        // 5. Handle Cover Promotion
        if (command.updateCover) {
            val metadataMap = stagedUpload.metadataJson?.let {
                @Suppress("UNCHECKED_CAST")
                objectMapper.readValue(it, Map::class.java) as Map<String, Any?>
            }
            val coverStorageKey = metadataMap?.get("coverStorageKey") as? String
            if (coverStorageKey != null) {
                val promotedCover = fileStoragePort.moveFile(coverStorageKey, "covers")
                uploadBookCoverUseCase.setCoverFromStorage(book.id!!, promotedCover.id)
            }
        }

        // 6. Cleanup Staging Record
        stagedRepository.delete(command.uploadId)

        return getBookUseCase.getBook(book.id!!)
    }

    private fun resolveAuthors(command: FinalizeImportCommand): List<Author> {
        val existingAuthorsByDirectId = command.authorIds.map { getAuthorUseCase.getAuthor(it) }
        
        val authorsFromNames = command.authorNames.map { name ->
            val (firstName, lastName) = splitAuthorName(name)
            
            // "Find or Create" logic to prevent duplicates
            val existingAuthor = authorRepository.findByName(firstName, lastName)
            if (existingAuthor != null) {
                logger.info { "Matched existing author by name: $name" }
                existingAuthor
            } else {
                logger.info { "Creating new author from import: $name" }
                saveAuthorUseCase.saveAuthor(Author(null, firstName, lastName, null, null, null))
            }
        }
        
        // Combine and ensure distinctness by ID (if an author was passed by both name and ID accidentally)
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
