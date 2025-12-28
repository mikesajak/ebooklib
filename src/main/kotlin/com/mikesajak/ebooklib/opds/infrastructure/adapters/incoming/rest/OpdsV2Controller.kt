package com.mikesajak.ebooklib.opds.infrastructure.adapters.incoming.rest

import com.mikesajak.ebooklib.author.application.ports.incoming.GetAuthorUseCase
import com.mikesajak.ebooklib.author.domain.model.AuthorId
import com.mikesajak.ebooklib.book.application.ports.incoming.*
import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.infrastructure.web.toDomainPagination
import com.mikesajak.ebooklib.opds.infrastructure.adapters.incoming.rest.dto.Feed
import com.mikesajak.ebooklib.opds.infrastructure.adapters.incoming.rest.dto.Link
import com.mikesajak.ebooklib.opds.infrastructure.adapters.incoming.rest.dto.OpdsMetadata
import com.mikesajak.ebooklib.opds.infrastructure.adapters.incoming.rest.dto.Publication
import com.mikesajak.ebooklib.series.application.ports.incoming.GetSeriesUseCase
import com.mikesajak.ebooklib.series.domain.model.SeriesId
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/opds/v2")
class OpdsV2Controller(
    private val getBookUseCase: GetBookUseCase,
    private val getAuthorUseCase: GetAuthorUseCase,
    private val getSeriesUseCase: GetSeriesUseCase,
    private val getBooksByAuthorUseCase: GetBooksByAuthorUseCase,
    private val getBooksBySeriesUseCase: GetBooksBySeriesUseCase,
    private val getBookCoverUseCase: GetBookCoverUseCase,
    private val getBookEbookFormatsUseCase: ListEbookFormatsUseCase,

    private val opdsBookMapper: OpdsBookMapper,
    private val opdsAuthorMapper: OpdsAuthorMapper,
    private val opdsSeriesMapper: OpdsSeriesMapper
) {
    companion object {
        const val OPDS_JSON_MEDIA_TYPE = "application/opds+json"
    }

    @GetMapping("/feed.json", produces = [OPDS_JSON_MEDIA_TYPE])
    fun getRootFeed(): Feed {
        val selfHref = "/opds/v2/feed.json"
        val allBooksHref = "/opds/v2/books/all.json"
        val newBooksHref = "/opds/v2/books/new.json"
        val authorsHref = "/opds/v2/authors/index.json"
        val seriesHref = "/opds/v2/series/index.json"

        val selfLink = Link(href = selfHref, type = OPDS_JSON_MEDIA_TYPE, rel = "self")

        val allBooksLink = Link(href = allBooksHref,
                                type = OPDS_JSON_MEDIA_TYPE,
                                rel = "http://opds-spec.org/explore",
                                title = "All Books")
        val newBooksLink = Link(href = newBooksHref,
                                type = OPDS_JSON_MEDIA_TYPE,
                                rel = "http://opds-spec.org/featured",
                                title = "New Books")
        val authorsLink = Link(href = authorsHref, type = OPDS_JSON_MEDIA_TYPE, rel = "subsection", title = "Authors")
        val seriesLink = Link(href = seriesHref, type = OPDS_JSON_MEDIA_TYPE, rel = "subsection", title = "Series")


        return Feed(
            metadata = OpdsMetadata(title = "Ebook Library Catalog"),
            links = listOf(selfLink),
            navigation = listOf(allBooksLink,
                                newBooksLink,
                                authorsLink,
                                seriesLink)
        )
    }

    @GetMapping("/books/all.json", produces = [OPDS_JSON_MEDIA_TYPE])
    fun getAllBooks(pageable: Pageable): Feed {
        val booksPage = getBookUseCase.getAllBooks(pageable.toDomainPagination())
        val publications = booksPage.content.map { publicationOf(it) }

        val metadata = OpdsMetadata(title = "All Books",
                                    numberOfItems = booksPage.totalElements.toInt(),
                                    itemsPerPage = booksPage.size,
                                    currentPage = booksPage.page)

        val links = createLinksForPage(booksPage, "/opds/v2/books/all.json")

        return Feed(metadata, links, publications)
    }

    @GetMapping("/books/new.json", produces = [OPDS_JSON_MEDIA_TYPE])
    fun getNewBooks(pageable: Pageable): Feed {
        val booksPage = getBookUseCase.getNewestBooks(pageable.toDomainPagination())
        val publications = booksPage.content.map { publicationOf(it) }

        val metadata = OpdsMetadata(title = "New Books",
                                    numberOfItems = booksPage.totalElements.toInt(),
                                    itemsPerPage = booksPage.size,
                                    currentPage = booksPage.page)

        val links = createLinksForPage(booksPage, "/opds/v2/books/new.json")

        return Feed(metadata, links, publications)
    }

    @GetMapping("/books/{bookIdValue}.json", produces = [OPDS_JSON_MEDIA_TYPE])
    fun getBook(@PathVariable bookIdValue: UUID): Publication {
        val bookId = com.mikesajak.ebooklib.book.domain.model.BookId(bookIdValue)
        val book = getBookUseCase.getBook(bookId)
        return publicationOf(book)
    }

    @GetMapping("/authors/index.json", produces = [OPDS_JSON_MEDIA_TYPE])
    fun getAuthorsIndex(pageable: Pageable): Feed {
        val authorsPage = getAuthorUseCase.getAllAuthors(pageable.toDomainPagination())
        val navigation = authorsPage.content.map { opdsAuthorMapper.toNavigationLink(it) }

        val metadata = OpdsMetadata(title = "Authors",
                                    numberOfItems = authorsPage.totalElements.toInt(),
                                    itemsPerPage = authorsPage.size,
                                    currentPage = authorsPage.page)

        val links = createLinksForPage(authorsPage, "/opds/v2/authors/index.json")

        return Feed(metadata, links, navigation = navigation)
    }

    @GetMapping("/authors/{authorId}/books.json", produces = [OPDS_JSON_MEDIA_TYPE])
    fun getAuthorBooks(@PathVariable authorId: UUID, pageable: Pageable): Feed {
        val author = getAuthorUseCase.getAuthor(AuthorId(authorId))
        val booksPage = getBooksByAuthorUseCase.getBooksByAuthor(AuthorId(authorId), pageable.toDomainPagination())
        val publications = booksPage.content.map { publicationOf(it) }

        val metadata = OpdsMetadata(title = "Books by ${author.firstName} ${author.lastName}",
                                    numberOfItems = booksPage.totalElements.toInt(),
                                    itemsPerPage = booksPage.size,
                                    currentPage = booksPage.page)

        val links = createLinksForPage(booksPage, "/opds/v2/authors/$authorId/books.json")

        return Feed(metadata, links, publications)
    }


    @GetMapping("/series/index.json", produces = [OPDS_JSON_MEDIA_TYPE])
    fun getSeriesIndex(pageable: Pageable): Feed {
        val seriesPage = getSeriesUseCase.getAllSeries(pageable.toDomainPagination())
        val navigation = seriesPage.content.map { opdsSeriesMapper.toNavigationLink(it) }

        val metadata = OpdsMetadata(title = "Series",
                                    numberOfItems = seriesPage.totalElements.toInt(),
                                    itemsPerPage = seriesPage.size,
                                    currentPage = seriesPage.page)

        val links = createLinksForPage(seriesPage, "/opds/v2/series/index.json")

        return Feed(metadata, links, navigation = navigation)
    }

    @GetMapping("/series/{seriesId}/books.json", produces = [OPDS_JSON_MEDIA_TYPE])
    fun getSeriesBooks(@PathVariable seriesId: UUID, pageable: Pageable): Feed {
        val series = getSeriesUseCase.getSeries(SeriesId(seriesId))
        val booksPage = getBooksBySeriesUseCase.getBooksOfSeries(SeriesId(seriesId), pageable.toDomainPagination())
        val publications = booksPage.content.map { publicationOf(it) }

        val metadata = OpdsMetadata(title = "Books in ${series.title}",
                                    numberOfItems = booksPage.totalElements.toInt(),
                                    itemsPerPage = booksPage.size,
                                    currentPage = booksPage.page
        )

        val links = createLinksForPage(booksPage, "/opds/v2/series/$seriesId/books.json")

        return Feed(
            metadata = metadata,
            links = links,
            publications = publications
        )
    }

    private fun createLinksForPage(booksPage: PaginatedResult<*>, baseHref: String): List<Link> =
        buildList {
            // Self link
            add(Link(href = "$baseHref?page=${booksPage.page}&size=${booksPage.size}",
                     type = OPDS_JSON_MEDIA_TYPE,
                     rel = "self"))


            // First link
            if (!booksPage.isFirst) {
                add(Link(href = "$baseHref?page=0&size=${booksPage.size}",
                         type = OPDS_JSON_MEDIA_TYPE,
                         rel = "first"))
            }

            // Previous link
            if (booksPage.hasPrevious) {
                add(Link(href = "$baseHref?page=${booksPage.page - 1}&size=${booksPage.size}",
                         type = OPDS_JSON_MEDIA_TYPE,
                         rel = "previous"))
            }

            // Next link
            if (booksPage.hasNext) {
                add(Link(href = "$baseHref?page=${booksPage.page + 1}&size=${booksPage.size}",
                         type = OPDS_JSON_MEDIA_TYPE,
                         rel = "next"))
            }

            // Last link
            if (!booksPage.isLast) {
                add(Link(href = "$baseHref?page=${booksPage.totalPages - 1}&size=${booksPage.size}",
                         type = OPDS_JSON_MEDIA_TYPE,
                         rel = "last"))
            }
        }

    private fun publicationOf(book: Book): Publication {
        val cover = book.id?.let { getBookCoverUseCase.getCoverIfExists(book.id)?.metadata }
        val formats = book.id?.let { getBookEbookFormatsUseCase.listFormatFiles(book.id) } ?: listOf()

        return opdsBookMapper.toPublication(book, cover, formats)
    }
}