package com.mikesajak.ebooklib.opds.infrastructure.adapters.incoming.rest

import com.mikesajak.ebooklib.author.application.ports.incoming.GetAuthorUseCase
import com.mikesajak.ebooklib.author.domain.model.AuthorId
import com.mikesajak.ebooklib.book.application.ports.incoming.GetBooksByAuthorUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.GetBooksBySeriesUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.GetBookUseCase
import com.mikesajak.ebooklib.infrastructure.web.toDomainPagination
import com.mikesajak.ebooklib.opds.infrastructure.adapters.incoming.rest.xml.dto.AtomContent
import com.mikesajak.ebooklib.opds.infrastructure.adapters.incoming.rest.xml.dto.AtomEntry
import com.mikesajak.ebooklib.opds.infrastructure.adapters.incoming.rest.xml.dto.AtomFeed
import com.mikesajak.ebooklib.opds.infrastructure.adapters.incoming.rest.xml.dto.AtomLink
import com.mikesajak.ebooklib.series.application.ports.incoming.GetSeriesUseCase
import com.mikesajak.ebooklib.series.domain.model.SeriesId
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_XML_VALUE
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@RestController
@RequestMapping("/opds/v1.2")
class OpdsV1Controller(
    private val getBookUseCase: GetBookUseCase,
    private val getAuthorUseCase: GetAuthorUseCase,
    private val getSeriesUseCase: GetSeriesUseCase,
    private val getBooksByAuthorUseCase: GetBooksByAuthorUseCase,
    private val getBooksBySeriesUseCase: GetBooksBySeriesUseCase,
    private val opdsV1Mapper: OpdsV1Mapper
) {
    companion object {
        const val OPDS_XML_MEDIA_TYPE = "application/atom+xml;profile=opds-catalog;kind=navigation"
    }

    private val dateFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.of("UTC"))

    @GetMapping("/catalog.xml", produces = [OPDS_XML_MEDIA_TYPE, APPLICATION_XML_VALUE])
    fun getRootFeed(): AtomFeed {
        val updated = ZonedDateTime.now().format(dateFormatter)

        val links = listOf(AtomLink(href = "/opds/v1.2/catalog.xml",
                                    rel = "self",
                                    type = OPDS_XML_MEDIA_TYPE),
                           AtomLink(href = "/opds/v1.2/catalog.xml",
                                    rel = "start",
                                    type = OPDS_XML_MEDIA_TYPE),
                           AtomLink(href = "/opds/v2/feed.json",
                                    rel = "alternate",
                                    type = OpdsV2Controller.OPDS_JSON_MEDIA_TYPE,
                                    title = "OPDS 2.0 Catalog"))

        val entries = listOf(AtomEntry(id = "urn:uuid:all-books",
                                       title = "All Books",
                                       updated = updated,
                                       links = listOf(AtomLink(href = "/opds/v1.2/books/all.xml",
                                                               rel = "http://opds-spec.org/sort/new",
                                                               type = OPDS_XML_MEDIA_TYPE)),
                                       content = AtomContent(type = "text",
                                                             text = "All books in the library")),
                             AtomEntry(id = "urn:uuid:new-books",
                                       title = "New Books",
                                       updated = updated,
                                       links = listOf(AtomLink(href = "/opds/v1.2/books/new.xml",
                                                               rel = "http://opds-spec.org/sort/new",
                                                               type = OPDS_XML_MEDIA_TYPE)),
                                       content = AtomContent(type = "text",
                                                             text = "Recently added books")
                             ),
                             AtomEntry(id = "urn:uuid:authors",
                                       title = "Authors",
                                       updated = updated,
                                       links = listOf(AtomLink(href = "/opds/v1.2/authors/index.xml",
                                                               rel = "http://opds-spec.org/sort/new", // TODO: Check if there is a better rel
                                                               type = OPDS_XML_MEDIA_TYPE)),
                                       content = AtomContent(type = "text",
                                                             text = "Browse by author")),
                             AtomEntry(id = "urn:uuid:series",
                                       title = "Series",
                                       updated = updated,
                                       links = listOf(AtomLink(href = "/opds/v1.2/series/index.xml",
                                                               rel = "http://opds-spec.org/sort/new",
                                                               type = OPDS_XML_MEDIA_TYPE)),
                                       content = AtomContent(type = "text",
                                                             text = "Browse by series")))

        return AtomFeed(id = "urn:uuid:root",
                        title = "Ebook Library Catalog",
                        updated = updated,
                        links = links,
                        entries = entries)
    }

    @GetMapping("/books/all.xml", produces = [OPDS_XML_MEDIA_TYPE, APPLICATION_XML_VALUE])
    fun getAllBooks(pageable: Pageable): AtomFeed {
        val booksPage = getBookUseCase.getAllBooks(pageable.toDomainPagination())
        val entries = booksPage.content.map { opdsV1Mapper.toEntry(it) }

        return createFeed(id = "urn:uuid:books:all",
                          title = "All Books",
                          updated = ZonedDateTime.now().format(dateFormatter),
                          entries = entries,
                          page = booksPage,
                          baseUrl = "/opds/v1.2/books/all.xml")
    }

    @GetMapping("/books/new.xml", produces = [OPDS_XML_MEDIA_TYPE, APPLICATION_XML_VALUE])
    fun getNewBooks(pageable: Pageable): AtomFeed {
        val booksPage = getBookUseCase.getNewestBooks(pageable.toDomainPagination())
        val entries = booksPage.content.map { opdsV1Mapper.toEntry(it) }

        return createFeed(id = "urn:uuid:books:new",
                          title = "New Books",
                          updated = ZonedDateTime.now().format(dateFormatter),
                          entries = entries,
                          page = booksPage,
                          baseUrl = "/opds/v1.2/books/new.xml")
    }

    @GetMapping("/authors/index.xml", produces = [OPDS_XML_MEDIA_TYPE, APPLICATION_XML_VALUE])
    fun getAuthorsIndex(pageable: Pageable): AtomFeed {
        val authorsPage = getAuthorUseCase.getAllAuthors(pageable.toDomainPagination())
        val entries = authorsPage.content.map { opdsV1Mapper.toEntry(it) }

        return createFeed(id = "urn:uuid:authors:index",
                          title = "Authors",
                          updated = ZonedDateTime.now().format(dateFormatter),
                          entries = entries,
                          page = authorsPage,
                          baseUrl = "/opds/v1.2/authors/index.xml")
    }

    @GetMapping("/authors/{authorId}/books.xml", produces = [OPDS_XML_MEDIA_TYPE, APPLICATION_XML_VALUE])
    fun getAuthorBooks(@PathVariable authorId: UUID, pageable: Pageable): AtomFeed {
        val author = getAuthorUseCase.getAuthor(AuthorId(authorId))
        val booksPage = getBooksByAuthorUseCase.getBooksByAuthor(AuthorId(authorId), pageable.toDomainPagination())
        val entries = booksPage.content.map { opdsV1Mapper.toEntry(it) }

        return createFeed(id = "urn:uuid:authors:$authorId:books",
                          title = "Books by ${author.firstName} ${author.lastName}",
                          updated = ZonedDateTime.now().format(dateFormatter),
                          entries = entries,
                          page = booksPage,
                          baseUrl = "/opds/v1.2/authors/$authorId/books.xml")
    }

    @GetMapping("/series/index.xml", produces = [OPDS_XML_MEDIA_TYPE, APPLICATION_XML_VALUE])
    fun getSeriesIndex(pageable: Pageable): AtomFeed {
        val seriesPage = getSeriesUseCase.getAllSeries(pageable.toDomainPagination())
        val entries = seriesPage.content.map { opdsV1Mapper.toEntry(it) }

        return createFeed(id = "urn:uuid:series:index",
                          title = "Series",
                          updated = ZonedDateTime.now().format(dateFormatter),
                          entries = entries,
                          page = seriesPage,
                          baseUrl = "/opds/v1.2/series/index.xml")
    }

    @GetMapping("/series/{seriesId}/books.xml", produces = [OPDS_XML_MEDIA_TYPE, APPLICATION_XML_VALUE])
    fun getSeriesBooks(@PathVariable seriesId: UUID, pageable: Pageable): AtomFeed {
        val series = getSeriesUseCase.getSeries(SeriesId(seriesId))
        val booksPage = getBooksBySeriesUseCase.getBooksOfSeries(SeriesId(seriesId), pageable.toDomainPagination())
        val entries = booksPage.content.map { opdsV1Mapper.toEntry(it) }

        return createFeed(id = "urn:uuid:series:$seriesId:books",
                          title = "Books in ${series.title}",
                          updated = ZonedDateTime.now().format(dateFormatter),
                          entries = entries,
                          page = booksPage,
                          baseUrl = "/opds/v1.2/series/$seriesId/books.xml"
        )
    }

    private fun <T> createFeed(id: String,
                               title: String,
                               updated: String,
                               entries: List<AtomEntry>,
                               page: com.mikesajak.ebooklib.common.domain.model.PaginatedResult<T>,
                               baseUrl: String
    ): AtomFeed {
        val links = buildList {
            // Self link
            add(AtomLink(href = "$baseUrl?page=${page.page}&size=${page.size}",
                         rel = "self",
                         type = OPDS_XML_MEDIA_TYPE))

            // First link
            if (!page.isFirst)
                add(AtomLink(href = "$baseUrl?page=0&size=${page.size}",
                             rel = "first",
                             type = OPDS_XML_MEDIA_TYPE))

            // Previous link
            if (page.hasPrevious)
                add(AtomLink(href = "$baseUrl?page=${page.page - 1}&size=${page.size}",
                             rel = "previous",
                             type = OPDS_XML_MEDIA_TYPE))

            // Next link
            if (page.hasNext)
                add(AtomLink(href = "$baseUrl?page=${page.page + 1}&size=${page.size}",
                             rel = "next",
                             type = OPDS_XML_MEDIA_TYPE))

            // Last link
            if (!page.isLast)
                add(AtomLink(href = "$baseUrl?page=${page.totalPages - 1}&size=${page.size}",
                             rel = "last",
                             type = OPDS_XML_MEDIA_TYPE))
        }

        return AtomFeed(id = id,
                        title = title,
                        updated = updated,
                        entries = entries,
                        links = links)
    }
}
