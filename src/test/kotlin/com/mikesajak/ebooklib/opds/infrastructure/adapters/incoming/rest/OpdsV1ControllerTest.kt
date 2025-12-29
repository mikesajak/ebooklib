package com.mikesajak.ebooklib.opds.infrastructure.adapters.incoming.rest

import com.mikesajak.ebooklib.author.application.ports.incoming.GetAuthorUseCase
import com.mikesajak.ebooklib.author.domain.model.Author
import com.mikesajak.ebooklib.author.domain.model.AuthorId
import com.mikesajak.ebooklib.book.application.ports.incoming.*
import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.domain.model.BookCover
import com.mikesajak.ebooklib.book.domain.model.BookCoverMetadata
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.book.domain.model.EbookFormatFile
import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.series.application.ports.incoming.GetSeriesUseCase
import com.mikesajak.ebooklib.series.domain.model.Series
import com.mikesajak.ebooklib.series.domain.model.SeriesId
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.util.*

@Suppress("HttpUrlsUsage")
@WebMvcTest(OpdsV1Controller::class)
@Import(OpdsV1Mapper::class)
class OpdsV1ControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var getBookUseCase: GetBookUseCase

    @MockitoBean
    private lateinit var getAuthorUseCase: GetAuthorUseCase

    @MockitoBean
    private lateinit var getSeriesUseCase: GetSeriesUseCase

    @MockitoBean
    private lateinit var getBooksByAuthorUseCase: GetBooksByAuthorUseCase

    @MockitoBean
    private lateinit var getBooksBySeriesUseCase: GetBooksBySeriesUseCase

    @MockitoBean
    private lateinit var getBookCoverUseCase: GetBookCoverUseCase

    @MockitoBean
    private lateinit var getBookEbookFormatsUseCase: ListEbookFormatsUseCase

    private val OPDS_XML_MEDIA_TYPE = "application/atom+xml;profile=opds-catalog;type=feed;kind=navigation"
    private val namespaces = mapOf("atom" to "http://www.w3.org/2005/Atom")

    private val authorId1 = AuthorId(UUID.randomUUID())
    private val author1 = Author(authorId1, "John", "Doe", null, null, null)

    private val seriesId1 = SeriesId(UUID.randomUUID())
    private val series1 = Series(seriesId1, "Test Series", "Some desc")

    private val bookId1 = BookId(UUID.randomUUID())
    private val book1CoverMetadata =
        BookCoverMetadata(UUID.randomUUID(), bookId1, "cover-storage-key-1", "cover1.jpg", "image/jpeg", 100L)
    private val book1Format1 = EbookFormatFile(UUID.randomUUID(),
        bookId1,
        "book1.epub",
        "application/epub+zip",
        200L,
        "EPUB",
        "epub-storage-key-1")
    private val book1 = Book(bookId1,
        "Book 1 Title",
        listOf(author1),
        LocalDate.now(),
        LocalDate.now(),
        "Publisher A",
        "Description A",
        series1,
        1,
        listOf("tag1"))

    private val bookId2 = BookId(UUID.randomUUID())
    private val book2 = Book(bookId2,
        "Book 2 Title",
        listOf(author1),
        LocalDate.now(),
        LocalDate.now(),
        "Publisher B",
        "Description B",
        null,
        null,
        listOf("tag2"))

    @BeforeEach
    fun setUp() {
        whenever(getAuthorUseCase.getAuthor(authorId1)).thenReturn(author1)
        whenever(getSeriesUseCase.getSeries(seriesId1)).thenReturn(series1)

        val book1Cover = BookCover(book1CoverMetadata, ByteArrayInputStream(ByteArray(0)))
        whenever(getBookCoverUseCase.getCoverIfExists(bookId1)).thenReturn(book1Cover)
        whenever(getBookEbookFormatsUseCase.listFormatFiles(bookId1)).thenReturn(listOf(book1Format1))

        whenever(getBookCoverUseCase.getCoverIfExists(bookId2)).thenReturn(null)
        whenever(getBookEbookFormatsUseCase.listFormatFiles(bookId2)).thenReturn(emptyList())
    }

    @Test
    fun `getRootFeed returns 200 OK and correct content`() {
        mockMvc.perform(get("/opds/v1.2/catalog.xml"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(OPDS_XML_MEDIA_TYPE))
            .andExpect(xpath("/atom:feed/atom:title", namespaces).string("Ebook Library Catalog"))
            .andExpect(xpath("/atom:feed/atom:subtitle", namespaces).string("Books in your library"))
            .andExpect(xpath("/atom:feed/atom:icon", namespaces).string("/favicon.ico"))
            .andExpect(xpath("/atom:feed/atom:link[@rel='search']/@href", namespaces).string("/opds/v1.2/search.xml"))
            .andExpect(xpath("/atom:feed/atom:entry", namespaces).nodeCount(4))
            .andExpect(xpath("/atom:feed/atom:entry[1]/atom:title", namespaces).string("All Books"))
            .andExpect(xpath("/atom:feed/atom:entry[1]/atom:link/@href", namespaces).string("/opds/v1.2/books/all.xml"))
            .andExpect(xpath("/atom:feed/atom:entry[2]/atom:title", namespaces).string("New Books"))
            .andExpect(xpath("/atom:feed/atom:entry[2]/atom:link/@href", namespaces).string("/opds/v1.2/books/new.xml"))
            .andExpect(xpath("/atom:feed/atom:entry[3]/atom:title", namespaces).string("Authors"))
            .andExpect(xpath("/atom:feed/atom:entry[3]/atom:link/@href", namespaces).string("/opds/v1.2/authors/index.xml"))
            .andExpect(xpath("/atom:feed/atom:entry[4]/atom:title", namespaces).string("Series"))
            .andExpect(xpath("/atom:feed/atom:entry[4]/atom:link/@href", namespaces).string("/opds/v1.2/series/index.xml"))
    }

    @Test
    fun `getAllBooks returns 200 OK and paginated books with cover and acquisition links`() {
        val paginatedBooks = PaginatedResult(listOf(book1, book2), 0, 2, 2, 1)
        whenever(getBookUseCase.getAllBooks(any())).thenReturn(paginatedBooks)

        mockMvc.perform(get("/opds/v1.2/books/all.xml?page=0&size=2"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(OPDS_XML_MEDIA_TYPE))
            .andExpect(xpath("/atom:feed/atom:title", namespaces).string("All Books"))
            .andExpect(xpath("/atom:feed/atom:entry", namespaces).nodeCount(2))
            
            // Check book1
            .andExpect(xpath("/atom:feed/atom:entry[1]/atom:title", namespaces).string(book1.title))
            .andExpect(xpath("/atom:feed/atom:entry[1]/atom:author/atom:author/atom:name", namespaces).string("John Doe"))
            .andExpect(xpath("/atom:feed/atom:entry[1]/atom:link[@rel='http://opds-spec.org/image']/@href", namespaces)
                .string("/api/books/${book1.id!!.value}/cover"))
             .andExpect(xpath("/atom:feed/atom:entry[1]/atom:link[@rel='http://opds-spec.org/acquisition']/@href", namespaces)
                .string("/api/books/${book1.id.value}/formats/${book1Format1.id}"))
             .andExpect(xpath("/atom:feed/atom:entry[1]/atom:link[@rel='http://opds-spec.org/acquisition']/@type", namespaces)
                .string(book1Format1.contentType))
                
            // Check book2
            .andExpect(xpath("/atom:feed/atom:entry[2]/atom:title", namespaces).string(book2.title))
            .andExpect(xpath("/atom:feed/atom:entry[2]/atom:link[@rel='http://opds-spec.org/image']", namespaces).doesNotExist())
            .andExpect(xpath("/atom:feed/atom:entry[2]/atom:link[@rel='http://opds-spec.org/acquisition']", namespaces).doesNotExist())
    }

    @Test
    fun `getNewBooks returns 200 OK and paginated books`() {
        val paginatedBooks = PaginatedResult(listOf(book1, book2), 0, 2, 2, 1)
        whenever(getBookUseCase.getNewestBooks(any())).thenReturn(paginatedBooks)

        mockMvc.perform(get("/opds/v1.2/books/new.xml?page=0&size=2"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(OPDS_XML_MEDIA_TYPE))
            .andExpect(xpath("/atom:feed/atom:title", namespaces).string("New Books"))
            .andExpect(xpath("/atom:feed/atom:entry", namespaces).nodeCount(2))
    }

    @Test
    fun `getAuthorsIndex returns 200 OK and paginated authors`() {
        val authorId2 = AuthorId(UUID.randomUUID())
        val author2 = Author(authorId2, "Jane", "Smith", null, null, null)
        val paginatedAuthors = PaginatedResult(listOf(author1, author2), 0, 2, 1, 2)
        whenever(getAuthorUseCase.getAllAuthors(any())).thenReturn(paginatedAuthors)

        mockMvc.perform(get("/opds/v1.2/authors/index.xml?page=0&size=2"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(OPDS_XML_MEDIA_TYPE))
            .andExpect(xpath("/atom:feed/atom:title", namespaces).string("Authors"))
            .andExpect(xpath("/atom:feed/atom:entry", namespaces).nodeCount(2))
            .andExpect(xpath("/atom:feed/atom:entry[1]/atom:title", namespaces).string("John Doe"))
            .andExpect(xpath("/atom:feed/atom:entry[1]/atom:link/@href", namespaces).string("/opds/v1.2/authors/${author1.id!!.value}/books.xml"))
            .andExpect(xpath("/atom:feed/atom:entry[2]/atom:title", namespaces).string("Jane Smith"))
            .andExpect(xpath("/atom:feed/atom:entry[2]/atom:link/@href", namespaces).string("/opds/v1.2/authors/${author2.id!!.value}/books.xml"))
    }

    @Test
    fun `getAuthorBooks returns 200 OK and paginated books`() {
        val paginatedBooks = PaginatedResult(listOf(book1, book2), 0, 2, 1, 2)
        whenever(getBooksByAuthorUseCase.getBooksByAuthor(eq(authorId1), any())).thenReturn(paginatedBooks)

        mockMvc.perform(get("/opds/v1.2/authors/${authorId1.value}/books.xml?page=0&size=2"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(OPDS_XML_MEDIA_TYPE))
            .andExpect(xpath("/atom:feed/atom:title", namespaces).string("Books by John Doe"))
            .andExpect(xpath("/atom:feed/atom:entry", namespaces).nodeCount(2))
    }

    @Test
    fun `getSeriesIndex returns 200 OK and paginated series`() {
        val seriesId2 = SeriesId(UUID.randomUUID())
        val series2 = Series(seriesId2, "Another Series", "Another desc")
        val paginatedSeries = PaginatedResult(listOf(series1, series2), 0, 2, 1, 2)
        whenever(getSeriesUseCase.getAllSeries(any())).thenReturn(paginatedSeries)

        mockMvc.perform(get("/opds/v1.2/series/index.xml?page=0&size=2"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(OPDS_XML_MEDIA_TYPE))
            .andExpect(xpath("/atom:feed/atom:title", namespaces).string("Series"))
            .andExpect(xpath("/atom:feed/atom:entry", namespaces).nodeCount(2))
            .andExpect(xpath("/atom:feed/atom:entry[1]/atom:title", namespaces).string("Test Series"))
            .andExpect(xpath("/atom:feed/atom:entry[1]/atom:link/@href", namespaces).string("/opds/v1.2/series/${series1.id!!.value}/books.xml"))
            .andExpect(xpath("/atom:feed/atom:entry[2]/atom:title", namespaces).string("Another Series"))
            .andExpect(xpath("/atom:feed/atom:entry[2]/atom:link/@href", namespaces).string("/opds/v1.2/series/${series2.id!!.value}/books.xml"))
    }

    @Test
    fun `getSeriesBooks returns 200 OK and paginated books`() {
        val paginatedBooks = PaginatedResult(listOf(book1), 0, 1, 1, 1)
        whenever(getBooksBySeriesUseCase.getBooksOfSeries(eq(seriesId1), any())).thenReturn(paginatedBooks)

        mockMvc.perform(get("/opds/v1.2/series/${seriesId1.value}/books.xml?page=0&size=2"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(OPDS_XML_MEDIA_TYPE))
            .andExpect(xpath("/atom:feed/atom:title", namespaces).string("Books in Test Series"))
            .andExpect(xpath("/atom:feed/atom:entry", namespaces).nodeCount(1))
    }
}
