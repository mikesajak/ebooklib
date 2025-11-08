package com.mikesajak.ebooklib.book.application.services

import com.mikesajak.ebooklib.book.application.ports.outgoing.BookRepositoryPort
import com.mikesajak.ebooklib.book.domain.exception.BookCoverNotFoundException
import com.mikesajak.ebooklib.book.domain.exception.BookNotFoundException
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.book.infrastructure.adapters.outgoing.persistence.BookCoverEntity
import com.mikesajak.ebooklib.book.infrastructure.adapters.outgoing.persistence.BookCoverJpaRepository
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileMetadata
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileStoragePort
import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.io.InputStream
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
@Transactional
class BookCoverService(
    private val bookRepository: BookRepositoryPort,
    private val bookCoverJpaRepository: BookCoverJpaRepository,
    private val fileStoragePort: FileStoragePort
) {

    fun uploadCover(bookId: BookId, fileContent: InputStream, originalFileName: String, contentType: String): FileMetadata {
        // 1. Verify book existence
        bookRepository.findById(bookId) ?: throw BookNotFoundException(bookId)

        // 2. Check for existing cover and delete if present
        val existingCover = bookCoverJpaRepository.findByBookId(bookId.value)
        if (existingCover != null) {
            logger.info { "Deleting existing cover for book ${bookId.value} with storage key ${existingCover.storageKey}" }
            fileStoragePort.deleteFile(existingCover.storageKey)
            bookCoverJpaRepository.delete(existingCover)
        }

        // 3. Upload new file to storage
        val fileMetadata = fileStoragePort.uploadFile(fileContent, originalFileName, contentType)
        logger.info { "Uploaded new cover file: ${fileMetadata.id} for book ${bookId.value}" }

        // 4. Save new cover metadata to DB
        val newBookCoverEntity = BookCoverEntity(
            id = UUID.fromString(fileMetadata.id),
            bookId = bookId.value,
            storageKey = fileMetadata.id, // Using fileMetadata.id as storageKey
            fileName = fileMetadata.fileName,
            contentType = fileMetadata.contentType,
            fileSize = fileMetadata.size
        )
        bookCoverJpaRepository.save(newBookCoverEntity)
        logger.info { "Saved new cover metadata for book ${bookId.value}" }

        return fileMetadata
    }

    fun getCover(bookId: BookId): Pair<InputStream, FileMetadata> {
        val bookCoverEntity = bookCoverJpaRepository.findByBookId(bookId.value)
            ?: throw BookCoverNotFoundException(bookId)

        val inputStream = fileStoragePort.downloadFile(bookCoverEntity.storageKey)
        val fileMetadata = FileMetadata(
            id = bookCoverEntity.id.toString(),
            fileName = bookCoverEntity.fileName,
            contentType = bookCoverEntity.contentType,
            size = bookCoverEntity.fileSize
        )
        return Pair(inputStream, fileMetadata)
    }

    fun deleteCover(bookId: BookId) {
        val bookCoverEntity = bookCoverJpaRepository.findByBookId(bookId.value)
            ?: throw BookCoverNotFoundException(bookId)

        logger.info { "Deleting cover file ${bookCoverEntity.storageKey} for book ${bookId.value}" }
        fileStoragePort.deleteFile(bookCoverEntity.storageKey)
        bookCoverJpaRepository.delete(bookCoverEntity)
        logger.info { "Deleted cover metadata for book ${bookId.value}" }
    }
}
