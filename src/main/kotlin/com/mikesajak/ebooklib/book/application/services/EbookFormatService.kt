package com.mikesajak.ebooklib.book.application.services

import com.mikesajak.ebooklib.book.application.ports.incoming.AddEbookFormatUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.DeleteEbookFormatUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.DownloadEbookFormatUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.ListEbookFormatsUseCase
import com.mikesajak.ebooklib.book.application.ports.outgoing.BookRepositoryPort
import com.mikesajak.ebooklib.book.application.ports.outgoing.EbookFormatFileRepositoryPort
import com.mikesajak.ebooklib.book.domain.exception.BookNotFoundException
import com.mikesajak.ebooklib.book.domain.exception.EbookFormatFileNotFoundException
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.book.domain.model.EbookFormatFile
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
    private val ebookFormatFileRepository: EbookFormatFileRepositoryPort,
    private val fileStoragePort: FileStoragePort
) : AddEbookFormatUseCase, ListEbookFormatsUseCase, DownloadEbookFormatUseCase, DeleteEbookFormatUseCase {

    override fun addFormatFile(
        bookId: BookId,
        fileContent: InputStream,
        originalFileName: String,
        contentType: String,
        formatType: String
    ): EbookFormatFile {
        // 1. Verify book existence
        bookRepository.findById(bookId) ?: throw BookNotFoundException(bookId)

        // 2. Upload new file to storage
        val fileMetadata = fileStoragePort.uploadFile(fileContent, originalFileName, contentType)
        logger.info { "Uploaded new ebook format file: ${fileMetadata.id} for book ${bookId.value}" }

        // 3. Save new format file metadata to DB
        val newEbookFormatFile = EbookFormatFile(
            id = UUID.fromString(fileMetadata.id),
            bookId = bookId,
            storageKey = fileMetadata.id, // Using fileMetadata.id as storageKey
            fileName = fileMetadata.fileName,
            contentType = fileMetadata.contentType,
            fileSize = fileMetadata.size,
            formatType = formatType
        )
        val savedEbookFormatFile = ebookFormatFileRepository.save(newEbookFormatFile)
        logger.info { "Saved new ebook format file metadata for book ${bookId.value}" }

        return savedEbookFormatFile
    }

    override fun listFormatFiles(bookId: BookId): List<EbookFormatFile> {
        // 1. Verify book existence
        bookRepository.findById(bookId) ?: throw BookNotFoundException(bookId)

        return ebookFormatFileRepository.findByBookId(bookId)
    }

    override fun downloadFormatFile(bookId: BookId, formatFileId: UUID): Pair<InputStream, EbookFormatFile> {
        val ebookFormatFile = ebookFormatFileRepository.findByBookIdAndId(bookId, formatFileId)
            ?: throw EbookFormatFileNotFoundException(bookId, formatFileId)

        val inputStream = fileStoragePort.downloadFile(ebookFormatFile.storageKey)
        return Pair(inputStream, ebookFormatFile)
    }

    override fun deleteFormatFile(bookId: BookId, formatFileId: UUID) {
        val ebookFormatFile = ebookFormatFileRepository.findByBookIdAndId(bookId, formatFileId)
            ?: throw EbookFormatFileNotFoundException(bookId, formatFileId)

        logger.info { "Deleting ebook format file ${ebookFormatFile.storageKey} for book ${bookId.value}" }
        fileStoragePort.deleteFile(ebookFormatFile.storageKey)
        ebookFormatFileRepository.delete(ebookFormatFile)
        logger.info { "Deleted ebook format file metadata for book ${bookId.value}" }
    }
}
