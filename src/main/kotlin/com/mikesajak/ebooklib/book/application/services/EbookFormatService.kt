package com.mikesajak.ebooklib.book.application.services

import com.mikesajak.ebooklib.book.application.ports.outgoing.BookRepositoryPort
import com.mikesajak.ebooklib.book.domain.exception.BookNotFoundException
import com.mikesajak.ebooklib.book.domain.exception.EbookFormatFileNotFoundException
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.book.infrastructure.adapters.outgoing.persistence.EbookFormatFileEntity
import com.mikesajak.ebooklib.book.infrastructure.adapters.outgoing.persistence.EbookFormatFileJpaRepository
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
class EbookFormatService(
    private val bookRepository: BookRepositoryPort,
    private val ebookFormatFileJpaRepository: EbookFormatFileJpaRepository,
    private val fileStoragePort: FileStoragePort
) {

    fun addFormatFile(
        bookId: BookId,
        fileContent: InputStream,
        originalFileName: String,
        contentType: String,
        formatType: String
    ): FileMetadata {
        // 1. Verify book existence
        bookRepository.findById(bookId) ?: throw BookNotFoundException(bookId)

        // 2. Upload new file to storage
        val fileMetadata = fileStoragePort.uploadFile(fileContent, originalFileName, contentType)
        logger.info { "Uploaded new ebook format file: ${fileMetadata.id} for book ${bookId.value}" }

        // 3. Save new format file metadata to DB
        val newEbookFormatFileEntity = EbookFormatFileEntity(
            id = UUID.fromString(fileMetadata.id),
            bookId = bookId.value,
            storageKey = fileMetadata.id, // Using fileMetadata.id as storageKey
            fileName = fileMetadata.fileName,
            contentType = fileMetadata.contentType,
            fileSize = fileMetadata.size,
            formatType = formatType
        )
        ebookFormatFileJpaRepository.save(newEbookFormatFileEntity)
        logger.info { "Saved new ebook format file metadata for book ${bookId.value}" }

        return fileMetadata
    }

    fun listFormatFiles(bookId: BookId): List<FileMetadata> {
        // 1. Verify book existence
        bookRepository.findById(bookId) ?: throw BookNotFoundException(bookId)

        return ebookFormatFileJpaRepository.findByBookId(bookId.value).map { entity ->
            FileMetadata(
                id = entity.id.toString(),
                fileName = entity.fileName,
                contentType = entity.contentType,
                size = entity.fileSize
            )
        }
    }

    fun downloadFormatFile(bookId: BookId, formatFileId: UUID): Pair<InputStream, FileMetadata> {
        val ebookFormatFileEntity = ebookFormatFileJpaRepository.findByBookIdAndId(bookId.value, formatFileId)
            ?: throw EbookFormatFileNotFoundException(bookId, formatFileId)

        val inputStream = fileStoragePort.downloadFile(ebookFormatFileEntity.storageKey)
        val fileMetadata = FileMetadata(
            id = ebookFormatFileEntity.id.toString(),
            fileName = ebookFormatFileEntity.fileName,
            contentType = ebookFormatFileEntity.contentType,
            size = ebookFormatFileEntity.fileSize
        )
        return Pair(inputStream, fileMetadata)
    }

    fun deleteFormatFile(bookId: BookId, formatFileId: UUID) {
        val ebookFormatFileEntity = ebookFormatFileJpaRepository.findByBookIdAndId(bookId.value, formatFileId)
            ?: throw EbookFormatFileNotFoundException(bookId, formatFileId)

        logger.info { "Deleting ebook format file ${ebookFormatFileEntity.storageKey} for book ${bookId.value}" }
        fileStoragePort.deleteFile(ebookFormatFileEntity.storageKey)
        ebookFormatFileJpaRepository.delete(ebookFormatFileEntity)
        logger.info { "Deleted ebook format file metadata for book ${bookId.value}" }
    }
}
