package com.mikesajak.ebooklib.importing.application.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.mikesajak.ebooklib.author.application.ports.incoming.GetAuthorUseCase
import com.mikesajak.ebooklib.author.domain.model.Author
import com.mikesajak.ebooklib.author.domain.model.AuthorId
import com.mikesajak.ebooklib.book.application.ports.incoming.AddBookUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.AddEbookFormatUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.GetBookUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.UpdateBookUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.UploadBookCoverUseCase
import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileMetadata
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileStoragePort
import com.mikesajak.ebooklib.importing.application.ports.incoming.FinalizeImportCommand
import com.mikesajak.ebooklib.importing.application.ports.outgoing.StagedEbookUploadRepositoryPort
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUpload
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadId
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadStatus
import com.mikesajak.ebooklib.series.application.ports.incoming.GetSeriesUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

class FinalizeImportServiceTest {

    private val stagedRepository = mockk<StagedEbookUploadRepositoryPort>()
    private val fileStoragePort = mockk<FileStoragePort>()
    private val getBookUseCase = mockk<GetBookUseCase>()
    private val addBookUseCase = mockk<AddBookUseCase>()
    private val updateBookUseCase = mockk<UpdateBookUseCase>()
    private val getAuthorUseCase = mockk<GetAuthorUseCase>()
    private val getSeriesUseCase = mockk<GetSeriesUseCase>()
    private val addEbookFormatUseCase = mockk<AddEbookFormatUseCase>()
    private val uploadBookCoverUseCase = mockk<UploadBookCoverUseCase>()
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    private val service = FinalizeImportService(
        stagedRepository, fileStoragePort, getBookUseCase, addBookUseCase,
        updateBookUseCase, getAuthorUseCase, getSeriesUseCase,
        addEbookFormatUseCase, uploadBookCoverUseCase, objectMapper
    )

    @Test
    fun `should finalize import as new book`() {
        // given
        val uploadId = UUID.randomUUID()
        val authorId = UUID.randomUUID()
        val stagedUpload = StagedEbookUpload(
            id = StagedEbookUploadId(uploadId),
            fileName = "the-hobbit.epub",
            contentType = "application/epub+zip",
            fileSize = 1000L,
            metadataJson = "{}",
            status = StagedEbookUploadStatus.PARSED,
            createdAt = Instant.now(),
            expiryAt = Instant.now().plusSeconds(3600)
        )

        val command = FinalizeImportCommand(
            uploadId = StagedEbookUploadId(uploadId),
            title = "The Hobbit",
            authorIds = listOf(AuthorId(authorId))
        )

        val author = Author(AuthorId(authorId), "J.R.R.", "Tolkien", null, null, null)
        val newBook = Book(BookId(UUID.randomUUID()), "The Hobbit", listOf(author), null, null, null, null, null, null)

        every { stagedRepository.findById(StagedEbookUploadId(uploadId)) } returns stagedUpload
        every { getAuthorUseCase.getAuthor(AuthorId(authorId)) } returns author
        every { addBookUseCase.addBook(any()) } returns newBook
        every { fileStoragePort.moveFile(uploadId.toString(), null) } returns FileMetadata("permanent-key", "the-hobbit.epub", "application/epub+zip", 1000L)
        every { addEbookFormatUseCase.addFormatFromStorage(newBook.id!!, "permanent-key", "EPUB") } returns mockk()
        every { stagedRepository.delete(StagedEbookUploadId(uploadId)) } returns Unit
        every { getBookUseCase.getBook(newBook.id!!) } returns newBook

        // when
        val result = service.finalize(command)

        // then
        assertThat(result).isEqualTo(newBook)
        verify { addBookUseCase.addBook(match { it.title == "The Hobbit" }) }
        verify { fileStoragePort.moveFile(uploadId.toString(), null) }
        verify { addEbookFormatUseCase.addFormatFromStorage(newBook.id!!, "permanent-key", "EPUB") }
        verify { stagedRepository.delete(StagedEbookUploadId(uploadId)) }
    }

    @Test
    fun `should finalize import for existing book`() {
        // given
        val uploadId = UUID.randomUUID()
        val bookId = UUID.randomUUID()
        val authorId = UUID.randomUUID()
        val stagedUpload = StagedEbookUpload(
            id = StagedEbookUploadId(uploadId),
            fileName = "the-hobbit.epub",
            contentType = "application/epub+zip",
            fileSize = 1000L,
            metadataJson = "{}",
            status = StagedEbookUploadStatus.PARSED,
            createdAt = Instant.now(),
            expiryAt = Instant.now().plusSeconds(3600)
        )

        val command = FinalizeImportCommand(
            uploadId = StagedEbookUploadId(uploadId),
            bookId = BookId(bookId),
            title = "The Hobbit (Updated)",
            authorIds = listOf(AuthorId(authorId))
        )

        val author = Author(AuthorId(authorId), "J.R.R.", "Tolkien", null, null, null)
        val existingBook = Book(BookId(bookId), "The Hobbit", listOf(author), null, null, null, null, null, null)
        val updatedBook = existingBook.copy(title = "The Hobbit (Updated)")

        every { stagedRepository.findById(StagedEbookUploadId(uploadId)) } returns stagedUpload
        every { getBookUseCase.getBook(BookId(bookId)) } returns existingBook
        every { getAuthorUseCase.getAuthor(AuthorId(authorId)) } returns author
        every { updateBookUseCase.updateBook(any()) } returns updatedBook
        every { fileStoragePort.moveFile(uploadId.toString(), null) } returns FileMetadata("permanent-key", "the-hobbit.epub", "application/epub+zip", 1000L)
        every { addEbookFormatUseCase.addFormatFromStorage(BookId(bookId), "permanent-key", "EPUB") } returns mockk()
        every { stagedRepository.delete(StagedEbookUploadId(uploadId)) } returns Unit
        every { getBookUseCase.getBook(BookId(bookId)) } returns updatedBook

        // when
        val result = service.finalize(command)

        // then
        assertThat(result.title).isEqualTo("The Hobbit (Updated)")
        verify { updateBookUseCase.updateBook(match { it.title == "The Hobbit (Updated)" }) }
        verify { stagedRepository.delete(StagedEbookUploadId(uploadId)) }
    }
}
