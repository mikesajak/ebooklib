package com.mikesajak.ebooklib.opds.infrastructure.adapters.incoming.rest

import com.mikesajak.ebooklib.author.application.ports.incoming.GetAuthorUseCase
import com.mikesajak.ebooklib.author.domain.model.Author
import com.mikesajak.ebooklib.author.domain.model.AuthorId
import com.mikesajak.ebooklib.book.application.ports.incoming.GetBookUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.GetBooksByAuthorUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.GetBooksBySeriesUseCase
import com.mikesajak.ebooklib.book.domain.model.Book
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDate
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
class OpdsControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var getBookUseCase: GetBookUseCase

    @MockBean
    private lateinit var getAuthorUseCase: GetAuthorUseCase

    @MockBean
    private lateinit var getSeriesUseCase: GetSeriesUseCase

    @MockBean
    private lateinit var getBooksByAuthorUseCase: GetBooksByAuthorUseCase

    @MockBean
    private lateinit var getBooksBySeriesUseCase: GetBooksBySeriesUseCase

    private val OPDS_JSON_MEDIA_TYPE = "application/opds+json"

    private val authorId1 = AuthorId(UUID.randomUUID())
    private val author1 = Author(authorId1, "John", "Doe", null, null, null)

    private val seriesId1 = SeriesId(UUID.randomUUID())
    private val series1 = Series(seriesId1, "Test Series", "Some desc")

    private val bookId1 = BookId(UUID.randomUUID())
    private val book1Cover =
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
    }

    @Test
    fun `getRootFeed returns 200 OK and correct content`() {
        mockMvc.perform(get("/opds/v2/feed.json"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(OPDS_JSON_MEDIA_TYPE))
                .andExpect(jsonPath("$.metadata.title", `is`("Ebook Library Catalog")))
                .andExpect(jsonPath("$.links[0].rel", `is`("self")))
                .andExpect(jsonPath("$.links[0].href", `is`("/opds/v2/feed.json")))
                .andExpect(jsonPath("$.navigation", hasSize<Any>(4)))
                .andExpect(jsonPath("$.navigation[0].title", `is`("All Books")))
                .andExpect(jsonPath("$.navigation[0].href", `is`("/opds/v2/books/all.json")))
                .andExpect(jsonPath("$.navigation[1].title", `is`("New Books")))
                .andExpect(jsonPath("$.navigation[1].href", `is`("/opds/v2/books/new.json")))
                .andExpect(jsonPath("$.navigation[2].title", `is`("Authors")))
                .andExpect(jsonPath("$.navigation[2].href", `is`("/opds/v2/authors/index.json")))
                .andExpect(jsonPath("$.navigation[3].title", `is`("Series")))
                .andExpect(jsonPath("$.navigation[3].href", `is`("/opds/v2/series/index.json")))
    }

    @Test
    fun `getAllBooks returns 200 OK and paginated books with cover and acquisition links`() {
        val paginatedBooks = PaginatedResult(listOf(book1, book2), 0, 2, 1, 2)
//        `when`(getBookUseCase.getAllBooks(any(PaginationRequest::class.java))).thenReturn(paginatedBooks)
        whenever(getBookUseCase.getAllBooks(any())).thenReturn(paginatedBooks)

        mockMvc.perform(get("/opds/v2/books/all.json?page=0&size=2"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(OPDS_JSON_MEDIA_TYPE))
                .andExpect(jsonPath("$.metadata.title", `is`("All Books")))
                .andExpect(jsonPath("$.metadata.number_of_items", `is`(2)))
                .andExpect(jsonPath("$.metadata.items_per_page", `is`(2)))
                .andExpect(jsonPath("$.metadata.current_page", `is`(0)))
                .andExpect(jsonPath("$.links", hasSize<Any>(3))) // self, next, last
                .andExpect(jsonPath("$.links[0].rel", `is`("self")))
                .andExpect(jsonPath("$.links[0].href", `is`("/opds/v2/books/all.json?page=0&size=2")))
                .andExpect(jsonPath("$.links[1].rel", `is`("next")))
                .andExpect(jsonPath("$.links[1].href", `is`("/opds/v2/books/all.json?page=1&size=2")))
                .andExpect(jsonPath("$.links[2].rel", `is`("last")))
                .andExpect(jsonPath("$.links[2].href", `is`("/opds/v2/books/all.json?page=0&size=2")))
                .andExpect(jsonPath("$.publications", hasSize<Any>(2)))

                // Check book1 publication details (with cover and format)
                .andExpect(jsonPath("$.publications[0].metadata.title", `is`(book1.title)))
                .andExpect(jsonPath("$.publications[0].metadata.author[0].name", `is`("John Doe")))
                .andExpect(jsonPath("$.publications[0].links", hasSize<Any>(2))) // self, acquisition
                .andExpect(jsonPath("$.publications[0].links[0].rel", `is`("self")))
                .andExpect(jsonPath("$.publications[0].links[0].href", `is`("/opds/v2/books/${book1.id!!.value}.json")))
                .andExpect(jsonPath("$.publications[0].links[1].rel", `is`("http://opds-spec.org/acquisition")))
                .andExpect(jsonPath("$.publications[0].links[1].href",
                                    `is`("/api/books/${book1.id!!.value}/formats/${book1Format1.id}")))
                .andExpect(jsonPath("$.publications[0].links[1].type", `is`(book1Format1.contentType)))
                .andExpect(jsonPath("$.publications[0].images", hasSize<Any>(1))) // cover
                .andExpect(jsonPath("$.publications[0].images[0].rel", `is`("http://opds-spec.org/image")))
                .andExpect(jsonPath("$.publications[0].images[0].href", `is`("/api/books/${book1.id!!.value}/cover")))
                .andExpect(jsonPath("$.publications[0].images[0].type", `is`(book1Cover.contentType)))

                // Check book2 publication details (without cover or format)
                .andExpect(jsonPath("$.publications[1].metadata.title", `is`(book2.title)))
                .andExpect(jsonPath("$.publications[1].links", hasSize<Any>(1))) // self only
                .andExpect(jsonPath("$.publications[1].images").doesNotExist()) // no images link
    }

    @Test
    fun `getAuthorsIndex returns 200 OK and paginated authors`() {
        val authorId2 = AuthorId(UUID.randomUUID())
        val author2 = Author(authorId2, "Jane", "Smith", null, null, null)
        val paginatedAuthors = PaginatedResult(listOf(author1, author2), 0, 2, 1, 2)
        whenever(getAuthorUseCase.getAllAuthors(any())).thenReturn(paginatedAuthors)

        mockMvc.perform(get("/opds/v2/authors/index.json?page=0&size=2"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(OPDS_JSON_MEDIA_TYPE))
                .andExpect(jsonPath("$.metadata.title", `is`("Authors")))
                .andExpect(jsonPath("$.navigation", hasSize<Any>(2)))
                .andExpect(jsonPath("$.navigation[0].title", `is`("John Doe")))
                .andExpect(jsonPath("$.navigation[0].href", `is`("/opds/v2/authors/${author1.id!!.value}/books.json")))
                .andExpect(jsonPath("$.navigation[1].title", `is`("Jane Smith")))
                .andExpect(jsonPath("$.navigation[1].href", `is`("/opds/v2/authors/${author2.id!!.value}/books.json")))
    }

    @Test
    fun `getSeriesIndex returns 200 OK and paginated series`() {
        val seriesId2 = SeriesId(UUID.randomUUID())
        val series2 = Series(seriesId2, "Another Series", "Another desc")
        val paginatedSeries = PaginatedResult(listOf(series1, series2), 0, 2, 1, 2)
        whenever(getSeriesUseCase.getAllSeries(any())).thenReturn(paginatedSeries)

        mockMvc.perform(get("/opds/v2/series/index.json?page=0&size=2"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(OPDS_JSON_MEDIA_TYPE))
                .andExpect(jsonPath("$.metadata.title", `is`("Series")))
                .andExpect(jsonPath("$.navigation", hasSize<Any>(2)))
                .andExpect(jsonPath("$.navigation[0].title", `is`("Test Series")))
                .andExpect(jsonPath("$.navigation[0].href", `is`("/opds/v2/series/${series1.id!!.value}/books.json")))
                .andExpect(jsonPath("$.navigation[1].title", `is`("Another Series")))
                .andExpect(jsonPath("$.navigation[1].href", `is`("/opds/v2/series/${series2.id!!.value}/books.json")))
    }

    @Test
    fun `getAuthorBooks returns 200 OK and paginated books for specific author`() {
        val paginatedBooks = PaginatedResult(listOf(book1, book2), 0, 2, 1, 2)
        whenever(getBooksByAuthorUseCase.getBooksByAuthor(eq(authorId1), any())).thenReturn(paginatedBooks)

        mockMvc.perform(get("/opds/v2/authors/${authorId1.value}/books.json?page=0&size=2"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(OPDS_JSON_MEDIA_TYPE))
                .andExpect(jsonPath("$.metadata.title", `is`("Books by John Doe")))
                .andExpect(jsonPath("$.publications", hasSize<Any>(2)))
                .andExpect(jsonPath("$.publications[0].metadata.title", `is`(book1.title)))
                .andExpect(jsonPath("$.publications[1].metadata.title", `is`(book2.title)))
    }

    @Test
    fun `getSeriesBooks returns 200 OK and paginated books for specific series`() {
        val paginatedBooks = PaginatedResult(listOf(book1), 0, 1, 1, 1)
        whenever(getBooksBySeriesUseCase.getBooksOfSeries(eq(seriesId1), any())).thenReturn(paginatedBooks)

        mockMvc.perform(get("/opds/v2/series/${seriesId1.value}/books.json?page=0&size=2"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(OPDS_JSON_MEDIA_TYPE))
                .andExpect(jsonPath("$.metadata.title", `is`("Books in Test Series")))
                .andExpect(jsonPath("$.publications", hasSize<Any>(1)))
                .andExpect(jsonPath("$.publications[0].metadata.title", `is`(book1.title)))
    }
}
