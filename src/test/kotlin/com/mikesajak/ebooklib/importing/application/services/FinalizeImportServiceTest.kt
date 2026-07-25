package com.mikesajak.ebooklib.importing.application.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.mikesajak.ebooklib.author.application.ports.incoming.GetAuthorUseCase
import com.mikesajak.ebooklib.author.application.ports.incoming.SaveAuthorUseCase
import com.mikesajak.ebooklib.author.application.ports.outgoing.AuthorRepositoryPort
import com.mikesajak.ebooklib.author.domain.model.Author
import com.mikesajak.ebooklib.author.domain.model.AuthorId
import com.mikesajak.ebooklib.book.application.ports.incoming.*
import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileMetadata
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileStoragePort
import com.mikesajak.ebooklib.importing.application.ports.incoming.FinalizeImportCommand
import com.mikesajak.ebooklib.importing.application.ports.incoming.ResolutionItemUseCase
import com.mikesajak.ebooklib.importing.application.ports.outgoing.StagedEbookUploadRepositoryPort
import com.mikesajak.ebooklib.importing.domain.model.*
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
    private val saveAuthorUseCase = mockk<SaveAuthorUseCase>()
    private val authorRepository = mockk<AuthorRepositoryPort>()
    private val getSeriesUseCase = mockk<GetSeriesUseCase>()
    private val addEbookFormatUseCase = mockk<AddEbookFormatUseCase>()
    private val uploadBookCoverUseCase = mockk<UploadBookCoverUseCase>()
    private val resolutionItemUseCase = mockk<ResolutionItemUseCase>()
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    private val service = FinalizeImportService(
        stagedRepository, fileStoragePort, getBookUseCase, addBookUseCase,
        updateBookUseCase, getAuthorUseCase, saveAuthorUseCase, authorRepository,
        getSeriesUseCase, addEbookFormatUseCase, uploadBookCoverUseCase,
        resolutionItemUseCase, objectMapper
    )

    @Test
    fun `should finalize import as new book`() {
        // given
        val uploadId = UUID.randomUUID()
        val authorId = UUID.randomUUID()
        val resolutionItemId = UUID.randomUUID()
        val stagedUpload = StagedEbookUpload(
            id = StagedEbookUploadId(uploadId),
            fileName = "the-hobbit.epub",
            contentType = "application/epub+zip",
            fileSize = 1000L,
            metadataJson = "{}",
            status = StagedEbookUploadStatus.PARSED,
            createdAt = Instant.now(),
            expiryAt = Instant.now().plusSeconds(3600),
            resolutionItemId = resolutionItemId
        )

        val command = FinalizeImportCommand(
            uploadId = StagedEbookUploadId(uploadId),
            title = "The Hobbit",
            authorIds = listOf(AuthorId(authorId))
        )

        val author = Author(AuthorId(authorId), "J.R.R.", "Tolkien", null, null, null)
        val newBook = Book(BookId(UUID.randomUUID()), "The Hobbit", listOf(author), null, null, null, null, null, null)

        every { stagedRepository.findById(StagedEbookUploadId(uploadId)) } returns stagedUpload
        every { stagedRepository.findByResolutionItemId(any()) } returns listOf(stagedUpload)
        every { getAuthorUseCase.getAuthor(AuthorId(authorId)) } returns author
        every { addBookUseCase.addBook(any()) } returns newBook
        every { fileStoragePort.getFileMetadata("staged/$uploadId") } returns FileMetadata("staged/$uploadId", "the-hobbit.epub", "application/epub+zip", 1000L)
        every { fileStoragePort.moveFile("staged/$uploadId", null) } returns FileMetadata("permanent-key", "the-hobbit.epub", "application/epub+zip", 1000L)
        every { addEbookFormatUseCase.addFormatFromStorage(newBook.id!!, "permanent-key", "EPUB", "the-hobbit.epub") } returns mockk()
        every { resolutionItemUseCase.updateResolvedItem(any(), any(), any(), any(), any()) } returns mockk()
        every { getBookUseCase.getBook(newBook.id!!) } returns newBook

        // when
        val result = service.finalize(command)

        // then
        assertThat(result).isEqualTo(newBook)
        verify { resolutionItemUseCase.updateResolvedItem(ResolutionItemId(resolutionItemId), eq("The Hobbit"), any(), eq(ResolutionItemStatus.RESOLVED), any()) }
        verify { addBookUseCase.addBook(match { it.title == "The Hobbit" }) }
        verify { fileStoragePort.moveFile("staged/$uploadId", null) }
        verify { addEbookFormatUseCase.addFormatFromStorage(newBook.id!!, "permanent-key", "EPUB", "the-hobbit.epub") }
    }

    @Test
    fun `should finalize import for existing book`() {
        // given
        val uploadId = UUID.randomUUID()
        val bookId = UUID.randomUUID()
        val authorId = UUID.randomUUID()
        val resolutionItemId = UUID.randomUUID()
        val stagedUpload = StagedEbookUpload(
            id = StagedEbookUploadId(uploadId),
            fileName = "the-hobbit.epub",
            contentType = "application/epub+zip",
            fileSize = 1000L,
            metadataJson = "{}",
            status = StagedEbookUploadStatus.PARSED,
            createdAt = Instant.now(),
            expiryAt = Instant.now().plusSeconds(3600),
            resolutionItemId = resolutionItemId
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
        every { stagedRepository.findByResolutionItemId(any()) } returns listOf(stagedUpload)
        every { getBookUseCase.getBook(BookId(bookId)) } returns existingBook
        every { getAuthorUseCase.getAuthor(AuthorId(authorId)) } returns author
        every { updateBookUseCase.updateBook(any()) } returns updatedBook
        every { fileStoragePort.getFileMetadata("staged/$uploadId") } returns FileMetadata("staged/$uploadId", "the-hobbit.epub", "application/epub+zip", 1000L)
        every { fileStoragePort.moveFile("staged/$uploadId", null) } returns FileMetadata("permanent-key", "the-hobbit.epub", "application/epub+zip", 1000L)
        every { addEbookFormatUseCase.addFormatFromStorage(BookId(bookId), "permanent-key", "EPUB", "the-hobbit.epub") } returns mockk()
        every { resolutionItemUseCase.updateResolvedItem(any(), any(), any(), any(), any()) } returns mockk()
        every { getBookUseCase.getBook(BookId(bookId)) } returns updatedBook

        // when
        val result = service.finalize(command)

        // then
        assertThat(result.title).isEqualTo("The Hobbit (Updated)")
        verify { resolutionItemUseCase.updateResolvedItem(ResolutionItemId(resolutionItemId), eq("The Hobbit (Updated)"), any(), eq(ResolutionItemStatus.RESOLVED), any()) }
        verify { updateBookUseCase.updateBook(match { it.title == "The Hobbit (Updated)" }) }
        verify { fileStoragePort.moveFile("staged/$uploadId", null) }
    }
}
