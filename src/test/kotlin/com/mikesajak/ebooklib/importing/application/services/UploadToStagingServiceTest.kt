package com.mikesajak.ebooklib.importing.application.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.mikesajak.ebooklib.author.domain.model.Author
import com.mikesajak.ebooklib.book.application.ports.incoming.GetBookUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.ListEbookFormatsUseCase
import com.mikesajak.ebooklib.book.application.ports.outgoing.BookRepositoryPort
import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileMetadata
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileStoragePort
import com.mikesajak.ebooklib.importing.application.ports.incoming.EbookMetadataExtractorUseCase
import com.mikesajak.ebooklib.importing.application.ports.outgoing.StagedEbookUploadRepositoryPort
import com.mikesajak.ebooklib.importing.domain.model.ExtractedCoverImage
import com.mikesajak.ebooklib.importing.domain.model.ExtractedEbookMetadata
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.util.*

class UploadToStagingServiceTest {

    private val fileStoragePort = mockk<FileStoragePort>()
    private val metadataExtractor = mockk<EbookMetadataExtractorUseCase>()
    private val getBookUseCase = mockk<GetBookUseCase>()
    private val listEbookFormatsUseCase = mockk<ListEbookFormatsUseCase>()
    private val bookRepository = mockk<BookRepositoryPort>()
    private val repository = mockk<StagedEbookUploadRepositoryPort>()
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    private val service = UploadToStagingService(
        fileStoragePort, metadataExtractor, getBookUseCase, listEbookFormatsUseCase,
        bookRepository, repository, objectMapper
    )

    @Test
    fun `should upload and parse ebook`() {
        // given
        val fileContent = "dummy content".toByteArray()
        val fileName = "test.epub"
        val contentType = "application/epub+zip"
        val fileId = "staged/" + UUID.randomUUID().toString()
        
        val extractedMetadata = ExtractedEbookMetadata(
            title = "Test Title",
            authors = listOf("Author 1"),
            creationDate = null,
            publicationDate = null,
            publisher = "Test Publisher",
            description = "Test Description",
            coverImage = ExtractedCoverImage("cover.jpg", "image/jpeg", "fake image data".toByteArray())
        )

        every { metadataExtractor.extract(any(), any(), any()) } returns extractedMetadata
        every { fileStoragePort.uploadFile(any(), any(), any(), any()) } returnsMany listOf(
            FileMetadata(fileId, fileName, contentType, fileContent.size.toLong()),
            FileMetadata("staged/covers/cover-id", "cover.jpg", "image/jpeg", 100L)
        )
        every { bookRepository.findByTitleContaining(any(), any()) } returns PaginatedResult(emptyList(), 0, 10, 0, 0)
        every { repository.save(any()) } answers { firstArg() }

        // when
        val result = service.upload(ByteArrayInputStream(fileContent), fileName, contentType)

        // then
        assertThat(result.id.value).isEqualTo(UUID.fromString(fileId.substringAfterLast('/')))
        assertThat(result.fileName).isEqualTo(fileName)
        assertThat(result.status).isEqualTo(StagedEbookUploadStatus.PARSED)
        
        val metadataMap = objectMapper.readValue(result.metadataJson, Map::class.java)
        assertThat(metadataMap["title"]).isEqualTo("Test Title")
        assertThat(metadataMap["coverStorageKey"]).isEqualTo("staged/covers/cover-id")

        verify(exactly = 1) { fileStoragePort.uploadFile(any(), fileName, contentType, "staged") }
        verify(exactly = 1) { fileStoragePort.uploadFile(any(), "cover.jpg", "image/jpeg", "staged/covers") }
        verify(exactly = 1) { repository.save(any()) }
    }

    @Test
    fun `should perform automated global matching when no currentBookId is provided`() {
        // given
        val fileContent = "dummy content".toByteArray()
        val fileName = "test.epub"
        val contentType = "application/epub+zip"
        val fileId = "staged/" + UUID.randomUUID().toString()

        val extractedMetadata = ExtractedEbookMetadata(
            title = "The Hobbit",
            authors = listOf("J.R.R. Tolkien"),
            creationDate = null,
            publicationDate = null,
            publisher = null,
            description = null
        )

        val matchingBook = Book(
            id = BookId(UUID.randomUUID()),
            title = "The Hobbit",
            authors = listOf(Author(null, "J.R.R.", "Tolkien", null, null, null)),
            creationDate = null, publicationDate = null, publisher = null, description = null, series = null, volume = null, labels = emptyList()
        )

        every { metadataExtractor.extract(any(), any(), any()) } returns extractedMetadata
        every { fileStoragePort.uploadFile(any(), any(), any(), any()) } returns FileMetadata(fileId, fileName, contentType, fileContent.size.toLong())
        every { bookRepository.findByTitleContaining("The Hobbit", any()) } returns PaginatedResult(listOf(matchingBook), 0, 10, 1, 1)
        every { repository.save(any()) } answers { firstArg() }

        // when
        val result = service.upload(ByteArrayInputStream(fileContent), fileName, contentType)

        // then
        val metadataMap = objectMapper.readValue(result.metadataJson, Map::class.java)
        val validation = metadataMap["validation"] as Map<*, *>
        val candidates = validation["candidates"] as List<*>
        
        assertThat(candidates).hasSize(1)
        val candidate = candidates[0] as Map<*, *>
        assertThat(candidate["title"]).isEqualTo("The Hobbit")
        assertThat(candidate["score"]).isEqualTo(100) // 80 (title) + 20 (author)
        assertThat(candidate["titleMatch"]).isEqualTo(true)
        assertThat(candidate["authorMatch"]).isEqualTo(true)
    }

    @Test
    fun `should rank candidates correctly based on partial title match`() {
        // given
        val extractedMetadata = ExtractedEbookMetadata(title = "Hobbit", authors = emptyList(), creationDate = null, publicationDate = null, publisher = null, description = null)
        val fileId = "staged/" + UUID.randomUUID().toString()
        
        val book1 = Book(BookId(UUID.randomUUID()), "The Hobbit", emptyList(), null, null, null, null, null, null, emptyList())
        val book2 = Book(BookId(UUID.randomUUID()), "Hobbit", emptyList(), null, null, null, null, null, null, emptyList())

        every { metadataExtractor.extract(any(), any(), any()) } returns extractedMetadata
        every { fileStoragePort.uploadFile(any(), any(), any(), any()) } returns FileMetadata(fileId, "fn", "ct", 10L)
        every { bookRepository.findByTitleContaining("Hobbit", any()) } returns PaginatedResult(listOf(book1, book2), 0, 10, 2, 2)
        every { repository.save(any()) } answers { firstArg() }

        // when
        val result = service.upload(ByteArrayInputStream("content".toByteArray()), "file.epub", "ct")

        // then
        val metadataMap = objectMapper.readValue(result.metadataJson, Map::class.java)
        val validation = metadataMap["validation"] as Map<*, *>
        val candidates = validation["candidates"] as List<*>
        
        assertThat(candidates).hasSize(2)
        val c1 = candidates[0] as Map<*, *>
        val c2 = candidates[1] as Map<*, *>
        
        assertThat(c1["title"]).isEqualTo("Hobbit")
        assertThat(c1["score"]).isEqualTo(80) // Exact title match
        
        assertThat(c2["title"]).isEqualTo("The Hobbit")
        assertThat(c2["score"]).isEqualTo(50) // Partial title match (from findByTitleContaining)
    }

    @Test
    fun `should handle targeted matching when currentBookId is provided`() {
        // given
        val bookId = UUID.randomUUID()
        val fileId = "staged/" + UUID.randomUUID().toString()
        val extractedMetadata = ExtractedEbookMetadata(title = "New Title", authors = emptyList(), creationDate = null, publicationDate = null, publisher = null, description = null)
        val existingBook = Book(BookId(bookId), "Old Title", emptyList(), null, null, null, null, null, null, emptyList())

        every { metadataExtractor.extract(any(), any(), any()) } returns extractedMetadata
        every { fileStoragePort.uploadFile(any(), any(), any(), any()) } returns FileMetadata(fileId, "fn", "ct", 10L)
        every { getBookUseCase.getBook(BookId(bookId)) } returns existingBook
        every { repository.save(any()) } answers { firstArg() }

        // when
        val result = service.upload(ByteArrayInputStream("content".toByteArray()), "file.epub", "ct", bookId)

        // then
        val metadataMap = objectMapper.readValue(result.metadataJson, Map::class.java)
        val validation = metadataMap["validation"] as Map<*, *>
        val candidates = validation["candidates"] as List<*>
        
        assertThat(candidates).hasSize(1)
        val c1 = candidates[0] as Map<*, *>
        assertThat(c1["bookId"]).isEqualTo(bookId.toString())
        assertThat(c1["titleMatch"]).isEqualTo(false)
        
        verify { getBookUseCase.getBook(BookId(bookId)) }
        verify(exactly = 0) { bookRepository.findByTitleContaining(any(), any()) }
    }
}
