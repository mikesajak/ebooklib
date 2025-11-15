package com.mikesajak.ebooklib.book.application.services

import com.mikesajak.ebooklib.book.application.ports.outgoing.BookCoverRepositoryPort
import com.mikesajak.ebooklib.book.application.ports.outgoing.BookRepositoryPort
import com.mikesajak.ebooklib.book.domain.exception.BookCoverNotFoundException
import com.mikesajak.ebooklib.book.domain.exception.BookNotFoundException
import com.mikesajak.ebooklib.book.domain.model.BookCover
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileMetadata
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileStoragePort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayInputStream
import java.util.*

class BookCoverServiceTest {
    private val bookRepository = mockk<BookRepositoryPort>()
    private val bookCoverRepository = mockk<BookCoverRepositoryPort>()
    private val fileStoragePort = mockk<FileStoragePort>()

    private val bookCoverService = BookCoverService(bookRepository, bookCoverRepository, fileStoragePort)

    @Test
    fun `should upload cover`() {
        // given
        val bookId = BookId(UUID.randomUUID())
        val fileContent = ByteArrayInputStream("test".toByteArray())
        val originalFileName = "cover.jpg"
        val contentType = "image/jpeg"
        val fileMetadata = FileMetadata(UUID.randomUUID().toString(), originalFileName, contentType, 4)

        every { bookRepository.findById(bookId) } returns mockk()
        every { bookCoverRepository.findByBookId(bookId) } returns null
        every { fileStoragePort.uploadFile(fileContent, originalFileName, contentType) } returns fileMetadata
        every { bookCoverRepository.save(any()) } returns mockk()

        // when
        val result = bookCoverService.uploadCover(bookId, fileContent, originalFileName, contentType)

        // then
        assertThat(result).isEqualTo(fileMetadata)
        verify { bookCoverRepository.save(any()) }
    }

    @Test
    fun `should throw BookNotFoundException when uploading cover for non-existent book`() {
        // given
        val bookId = BookId(UUID.randomUUID())
        val fileContent = ByteArrayInputStream("test".toByteArray())
        val originalFileName = "cover.jpg"
        val contentType = "image/jpeg"

        every { bookRepository.findById(bookId) } returns null

        // when, then
        assertThrows<BookNotFoundException> {
            bookCoverService.uploadCover(bookId, fileContent, originalFileName, contentType)
        }
    }

    @Test
    fun `should get cover`() {
        // given
        val bookId = BookId(UUID.randomUUID())
        val bookCover = BookCover(UUID.randomUUID(), bookId, "storageKey", "cover.jpg", "image/jpeg", 123)
        val inputStream = ByteArrayInputStream("test".toByteArray())

        every { bookCoverRepository.findByBookId(bookId) } returns bookCover
        every { fileStoragePort.downloadFile("storageKey") } returns inputStream

        // when
        val (resultStream, resultMetadata) = bookCoverService.getCover(bookId)

        // then
        assertThat(resultStream).isEqualTo(inputStream)
        assertThat(resultMetadata.id).isEqualTo(bookCover.id.toString())
        assertThat(resultMetadata.fileName).isEqualTo(bookCover.fileName)
        assertThat(resultMetadata.contentType).isEqualTo(bookCover.contentType)
        assertThat(resultMetadata.size).isEqualTo(bookCover.fileSize)
    }

    @Test
    fun `should throw BookCoverNotFoundException when getting non-existent cover`() {
        // given
        val bookId = BookId(UUID.randomUUID())

        every { bookCoverRepository.findByBookId(bookId) } returns null

        // when, then
        assertThrows<BookCoverNotFoundException> {
            bookCoverService.getCover(bookId)
        }
    }

    @Test
    fun `should delete cover`() {
        // given
        val bookId = BookId(UUID.randomUUID())
        val bookCover = BookCover(UUID.randomUUID(), bookId, "storageKey", "cover.jpg", "image/jpeg", 123)

        every { bookCoverRepository.findByBookId(bookId) } returns bookCover
        every { fileStoragePort.deleteFile("storageKey") } returns Unit
        every { bookCoverRepository.delete(bookCover) } returns Unit

        // when
        bookCoverService.deleteCover(bookId)

        // then
        verify { fileStoragePort.deleteFile("storageKey") }
        verify { bookCoverRepository.delete(bookCover) }
    }

    @Test
    fun `should return true when cover exists`() {
        // given
        val bookId = BookId(UUID.randomUUID())
        every { bookCoverRepository.existsByBookId(bookId) } returns true

        // when
        val result = bookCoverService.hasCover(bookId)

        // then
        assertThat(result).isTrue
    }

    @Test
    fun `should return false when cover does not exist`() {
        // given
        val bookId = BookId(UUID.randomUUID())
        every { bookCoverRepository.existsByBookId(bookId) } returns false

        // when
        val result = bookCoverService.hasCover(bookId)

        // then
        assertThat(result).isFalse
    }
}
