package com.mikesajak.ebooklib.book.application.services

import com.mikesajak.ebooklib.book.application.ports.incoming.DeleteBookCoverUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.GetBookCoverUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.HasBookCoverUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.UploadBookCoverUseCase
import com.mikesajak.ebooklib.book.application.ports.outgoing.BookCoverRepositoryPort
import com.mikesajak.ebooklib.book.application.ports.outgoing.BookRepositoryPort
import com.mikesajak.ebooklib.book.domain.exception.BookCoverFileMissingException
import com.mikesajak.ebooklib.book.domain.exception.BookCoverNotFoundException
import com.mikesajak.ebooklib.book.domain.exception.BookNotFoundException
import com.mikesajak.ebooklib.book.domain.model.BookCover
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileMetadata
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileStoragePort
import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import java.io.InputStream
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
@Transactional
class BookCoverService(
    private val bookRepository: BookRepositoryPort,
    private val bookCoverRepository: BookCoverRepositoryPort,
    private val fileStoragePort: FileStoragePort
) : UploadBookCoverUseCase, GetBookCoverUseCase, DeleteBookCoverUseCase, HasBookCoverUseCase {

    override fun uploadCover(bookId: BookId, fileContent: InputStream, originalFileName: String, contentType: String): FileMetadata {
        // 1. Verify book existence
        bookRepository.findById(bookId) ?: throw BookNotFoundException(bookId)

        // 2. Check for existing cover and delete if present
        val existingCover = bookCoverRepository.findByBookId(bookId)
        if (existingCover != null) {
            logger.info { "Deleting existing cover for book ${bookId.value} with storage key ${existingCover.storageKey}" }
            fileStoragePort.deleteFile(existingCover.storageKey)
            bookCoverRepository.delete(existingCover)
        }

        // 3. Upload new file to storage
        val fileMetadata = fileStoragePort.uploadFile(fileContent, originalFileName, contentType)
        logger.info { "Uploaded new cover file: ${fileMetadata.id} for book ${bookId.value}" }

        // 4. Save new cover metadata to DB
        val newBookCover = BookCover(
            id = UUID.fromString(fileMetadata.id),
            bookId = bookId,
            storageKey = fileMetadata.id, // Using fileMetadata.id as storageKey
            fileName = fileMetadata.fileName,
            contentType = fileMetadata.contentType,
            fileSize = fileMetadata.size
        )
        bookCoverRepository.save(newBookCover)
        logger.info { "Saved new cover metadata for book ${bookId.value}" }

        return fileMetadata
    }

    override fun getCover(bookId: BookId): Pair<InputStream, FileMetadata> {
        val bookCover = bookCoverRepository.findByBookId(bookId)
            ?: throw BookCoverNotFoundException(bookId)

        val inputStream = try {
            fileStoragePort.downloadFile(bookCover.storageKey)
        } catch (e: NoSuchKeyException) {
            logger.warn(e) { "Book cover file ${bookCover.storageKey} for book ${bookId.value} not found in storage, but metadata exists." }
            throw BookCoverFileMissingException(bookId)
        }

        val fileMetadata = FileMetadata(
            id = bookCover.id.toString(),
            fileName = bookCover.fileName,
            contentType = bookCover.contentType,
            size = bookCover.fileSize
        )
        return Pair(inputStream, fileMetadata)
    }

    override fun deleteCover(bookId: BookId) {
        val bookCover = bookCoverRepository.findByBookId(bookId)
            ?: throw BookCoverNotFoundException(bookId)

        try {
            logger.info { "Attempting to delete cover file ${bookCover.storageKey} for book ${bookId.value} from storage." }
            fileStoragePort.deleteFile(bookCover.storageKey)
        } catch (e: NoSuchKeyException) {
            logger.warn(e) { "Book cover file ${bookCover.storageKey} for book ${bookId.value} not found in storage. Proceeding with metadata deletion." }
        } catch (e: Exception) {
            logger.error(e) { "Error deleting cover file ${bookCover.storageKey} for book ${bookId.value} from storage. Proceeding with metadata deletion." }
        }

        bookCoverRepository.delete(bookCover)
        logger.info { "Deleted cover metadata for book ${bookId.value}" }
    }

    override fun hasCover(bookId: BookId): Boolean {
        return bookCoverRepository.existsByBookId(bookId)
    }
}
