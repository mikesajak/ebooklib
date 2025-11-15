package com.mikesajak.ebooklib.book.application.services

import com.mikesajak.ebooklib.book.application.ports.outgoing.BookRepositoryPort
import com.mikesajak.ebooklib.book.application.ports.outgoing.EbookFormatFileRepositoryPort
import com.mikesajak.ebooklib.book.domain.exception.BookNotFoundException
import com.mikesajak.ebooklib.book.domain.exception.EbookFormatFileNotFoundException
import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.book.domain.model.EbookFormatFile
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileMetadata
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileStoragePort
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.io.ByteArrayInputStream
import java.util.*

@ExtendWith(MockitoExtension::class)
class EbookFormatServiceTest {

    @Mock
    private lateinit var bookRepository: BookRepositoryPort

    @Mock
    private lateinit var ebookFormatFileRepository: EbookFormatFileRepositoryPort

    @Mock
    private lateinit var fileStoragePort: FileStoragePort

    private lateinit var ebookFormatService: EbookFormatService

    @BeforeEach
    fun setUp() {
        ebookFormatService = EbookFormatService(bookRepository, ebookFormatFileRepository, fileStoragePort)
    }

    @Test
    fun `should add format file successfully`() {
        // Given
        val bookId = BookId(UUID.randomUUID())
        val fileContent = ByteArrayInputStream("test content".toByteArray())
        val originalFileName = "test.epub"
        val contentType = "application/ebok+zip"
        val formatType = "EPUB"
        val book = Book(id = bookId, title = "Test Book", authors = emptyList(),
                        creationDate = null, publicationDate = null, publisher = null,
                        description = null, series = null, volume = null, labels = emptyList())
        val fileMetadata = FileMetadata(UUID.randomUUID().toString(), originalFileName, contentType, 12L)
        val expectedEbookFormatFile = EbookFormatFile(
                id = UUID.fromString(fileMetadata.id),
                bookId = bookId,
                storageKey = fileMetadata.id,
                fileName = fileMetadata.fileName,
                contentType = fileMetadata.contentType,
                fileSize = fileMetadata.size,
                formatType = formatType
        )

        whenever(bookRepository.findById(bookId))
                .thenReturn(book)
        whenever(fileStoragePort.uploadFile(any(), eq(originalFileName), eq(contentType)))
                .thenReturn(fileMetadata)
        whenever(ebookFormatFileRepository.save(any()))
                .thenReturn(expectedEbookFormatFile)

        // When
        val result = ebookFormatService.addFormatFile(bookId, fileContent, originalFileName, contentType, formatType)

        // Then
        assertThat(result).isEqualTo(expectedEbookFormatFile)
        verify(bookRepository).findById(bookId)
        verify(fileStoragePort).uploadFile(any(), eq(originalFileName), eq(contentType))
        verify(ebookFormatFileRepository).save(any())
    }

    @Test
    fun `should throw BookNotFoundException when adding format file to non-existent book`() {
        // Given
        val bookId = BookId(UUID.randomUUID())
        val fileContent = ByteArrayInputStream("test content".toByteArray())
        val originalFileName = "test.epub"
        val contentType = "application/epub+zip"
        val formatType = "EPUB"

        whenever(bookRepository.findById(bookId)).thenReturn(null)

        // When / Then
        assertThatThrownBy {
            ebookFormatService.addFormatFile(bookId, fileContent, originalFileName, contentType, formatType)
        }.isInstanceOf(BookNotFoundException::class.java)

        verify(bookRepository).findById(bookId)
        verifyNoInteractions(fileStoragePort)
        verifyNoInteractions(ebookFormatFileRepository)
    }

    @Test
    fun `should list format files successfully`() {
        // Given
        val bookId = BookId(UUID.randomUUID())
        val book = Book(bookId, "Test Book", emptyList(),
                        null, null, null,
                        null, null, null)
        val ebookFormatFile1 = EbookFormatFile(
                id = UUID.randomUUID(),
                bookId = bookId,
                storageKey = "key1",
                fileName = "file1.epub",
                contentType = "application/epub+zip",
                fileSize = 100L,
                formatType = "EPUB"
        )
        val ebookFormatFile2 = EbookFormatFile(
                id = UUID.randomUUID(),
                bookId = bookId,
                storageKey = "key2",
                fileName = "file2.mobi",
                contentType = "application/x-mobipocket-ebook",
                fileSize = 200L,
                formatType = "MOBI"
        )
        val expectedList = listOf(ebookFormatFile1, ebookFormatFile2)

        whenever(bookRepository.findById(bookId)).thenReturn(book)
        whenever(ebookFormatFileRepository.findByBookId(bookId)).thenReturn(expectedList)

        // When
        val result = ebookFormatService.listFormatFiles(bookId)

        // Then
        assertThat(result).isEqualTo(expectedList)
        verify(bookRepository).findById(bookId)
        verify(ebookFormatFileRepository).findByBookId(bookId)
    }

    @Test
    fun `should throw BookNotFoundException when listing format files for non-existent book`() {
        // Given
        val bookId = BookId(UUID.randomUUID())

        whenever(bookRepository.findById(bookId)).thenReturn(null)

        // When / Then
        assertThatThrownBy {
            ebookFormatService.listFormatFiles(bookId)
        }.isInstanceOf(BookNotFoundException::class.java)

        verify(bookRepository).findById(bookId)
        verifyNoInteractions(ebookFormatFileRepository)
    }

    @Test
    fun `should return empty list when no format files found for book`() {
        // Given
        val bookId = BookId(UUID.randomUUID())
        val book = Book(bookId, "Test Book", emptyList(),
                        null, null, null,
                        null, null, null)

        whenever(bookRepository.findById(bookId)).thenReturn(book)
        whenever(ebookFormatFileRepository.findByBookId(bookId)).thenReturn(emptyList())

        // When
        val result = ebookFormatService.listFormatFiles(bookId)

        // Then
        assertThat(result).isEmpty()
        verify(bookRepository).findById(bookId)
        verify(ebookFormatFileRepository).findByBookId(bookId)
    }

    @Test
    fun `should download format file successfully`() {
        // Given
        val bookId = BookId(UUID.randomUUID())
        val formatFileId = UUID.randomUUID()
        val ebookFormatFile = EbookFormatFile(
                id = formatFileId,
                bookId = bookId,
                storageKey = "storage-key-123",
                fileName = "file.epub",
                contentType = "application/epub+zip",
                fileSize = 123L,
                formatType = "EPUB"
        )
        val expectedInputStream = ByteArrayInputStream("file content".toByteArray())

        whenever(ebookFormatFileRepository.findByBookIdAndId(bookId, formatFileId)).thenReturn(ebookFormatFile)
        whenever(fileStoragePort.downloadFile(ebookFormatFile.storageKey)).thenReturn(expectedInputStream)

        // When
        val (inputStream, downloadedFile) = ebookFormatService.downloadFormatFile(bookId, formatFileId)

        // Then
        assertThat(downloadedFile).isEqualTo(ebookFormatFile)
        assertThat(inputStream).isEqualTo(expectedInputStream)
        verify(ebookFormatFileRepository).findByBookIdAndId(bookId, formatFileId)
        verify(fileStoragePort).downloadFile(ebookFormatFile.storageKey)
    }

    @Test
    fun `should throw EbookFormatFileNotFoundException when downloading non-existent format file`() {
        // Given
        val bookId = BookId(UUID.randomUUID())
        val formatFileId = UUID.randomUUID()

        whenever(ebookFormatFileRepository.findByBookIdAndId(bookId, formatFileId)).thenReturn(null)

        // When / Then
        assertThatThrownBy {
            ebookFormatService.downloadFormatFile(bookId, formatFileId)
        }.isInstanceOf(EbookFormatFileNotFoundException::class.java)

        verify(ebookFormatFileRepository).findByBookIdAndId(bookId, formatFileId)
        verifyNoInteractions(fileStoragePort)
    }

    @Test
    fun `should delete format file successfully`() {
        // Given
        val bookId = BookId(UUID.randomUUID())
        val formatFileId = UUID.randomUUID()
        val ebookFormatFile = EbookFormatFile(
                id = formatFileId,
                bookId = bookId,
                storageKey = "storage-key-123",
                fileName = "file.epub",
                contentType = "application/epub+zip",
                fileSize = 123L,
                formatType = "EPUB"
        )

        whenever(ebookFormatFileRepository.findByBookIdAndId(bookId, formatFileId)).thenReturn(ebookFormatFile)
        doNothing().whenever(fileStoragePort).deleteFile(ebookFormatFile.storageKey)
        doNothing().whenever(ebookFormatFileRepository).delete(ebookFormatFile)

        // When
        ebookFormatService.deleteFormatFile(bookId, formatFileId)

        // Then
        verify(ebookFormatFileRepository).findByBookIdAndId(bookId, formatFileId)
        verify(fileStoragePort).deleteFile(ebookFormatFile.storageKey)
        verify(ebookFormatFileRepository).delete(ebookFormatFile)
    }

    @Test
    fun `should throw EbookFormatFileNotFoundException when deleting non-existent format file`() {
        // Given
        val bookId = BookId(UUID.randomUUID())
        val formatFileId = UUID.randomUUID()

        whenever(ebookFormatFileRepository.findByBookIdAndId(bookId, formatFileId)).thenReturn(null)

        // When / Then
        assertThatThrownBy {
            ebookFormatService.deleteFormatFile(bookId, formatFileId)
        }.isInstanceOf(EbookFormatFileNotFoundException::class.java)

        verify(ebookFormatFileRepository).findByBookIdAndId(bookId, formatFileId)
        verifyNoInteractions(fileStoragePort)
    }
}