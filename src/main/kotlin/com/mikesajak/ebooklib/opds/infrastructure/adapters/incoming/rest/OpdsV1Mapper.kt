package com.mikesajak.ebooklib.opds.infrastructure.adapters.incoming.rest

import com.mikesajak.ebooklib.author.domain.model.Author
import com.mikesajak.ebooklib.book.application.ports.incoming.GetBookCoverUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.ListEbookFormatsUseCase
import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.domain.model.BookCoverMetadata
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.book.domain.model.EbookFormatFile
import com.mikesajak.ebooklib.opds.infrastructure.adapters.incoming.rest.dto.AtomAuthor
import com.mikesajak.ebooklib.opds.infrastructure.adapters.incoming.rest.dto.AtomContent
import com.mikesajak.ebooklib.opds.infrastructure.adapters.incoming.rest.dto.AtomEntry
import com.mikesajak.ebooklib.opds.infrastructure.adapters.incoming.rest.dto.AtomLink
import com.mikesajak.ebooklib.series.domain.model.Series
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Component
class OpdsV1Mapper(
        private val getBookCoverUseCase: GetBookCoverUseCase,
        private val getBookEbookFormatsUseCase: ListEbookFormatsUseCase
) {

    private val dateFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.of("UTC"))

    fun toSummaryEntry(book: Book): AtomEntry {
        val coverLinks = coverAtomLinks(book)
        val formatLinks = formatAtomLinks(book)
        val detailLink = AtomLink(href = "/opds/v1.2/books/${book.id!!.value}.xml",
                                  rel = "alternate",
                                  type = OpdsV1Controller.OPDS_XML_MEDIA_TYPE)

        return AtomEntry(id = "urn:uuid:${book.id.value}",
                         title = book.title,
                         updated = (book.creationDate?.atStartOfDay(ZoneId.of("UTC"))
                                    ?: ZonedDateTime.now()).format(dateFormatter),
                         author = book.authors.map { AtomAuthor(name = "${it.firstName} ${it.lastName}") },
                         summary = book.description,
                         links = coverLinks + formatLinks + detailLink,
                         published = book.publicationDate?.atStartOfDay(ZoneId.of("UTC"))?.format(dateFormatter)
        )
    }

    fun toFullEntry(book: Book): AtomEntry {
        val coverLinks = coverAtomLinks(book)
        val formatLinks = formatAtomLinks(book)
        val selfLink = AtomLink(href = "/opds/v1.2/books/${book.id!!.value}.xml",
                                rel = "self",
                                type = OpdsV1Controller.OPDS_XML_MEDIA_TYPE)

        return AtomEntry(id = "urn:uuid:${book.id!!.value}",
                         title = book.title,
                         updated = (book.creationDate?.atStartOfDay(ZoneId.of("UTC"))
                                    ?: ZonedDateTime.now()).format(dateFormatter),
                         author = book.authors.map { AtomAuthor(name = "${it.firstName} ${it.lastName}") },
                         summary = book.description,
                         links = coverLinks + formatLinks + selfLink,
                         published = book.publicationDate?.atStartOfDay(ZoneId.of("UTC"))?.format(dateFormatter),
                         content = book.description?.let { AtomContent(type = "text", text = it) }
        )
    }

    fun toEntry(book: Book): AtomEntry = toFullEntry(book)

    private fun formatAtomLinks(book: Book): List<AtomLink> {
        return book.id?.let {
            getBookEbookFormatsUseCase.listFormatFiles(book.id)
                    .map { format -> mkFormatLink(book.id, format, book.creationDate) }
        } ?: listOf()
    }

    @Suppress("HttpUrlsUsage")
    private fun mkFormatLink(id: BookId, format: EbookFormatFile, creationDate: LocalDate?): AtomLink {
        val mtime = (creationDate?.atStartOfDay(ZoneId.of("UTC")) ?: ZonedDateTime.now()).format(dateFormatter)
        return AtomLink(
            href = "/api/books/${id.value}/formats/${format.id}/download",
            rel = "http://opds-spec.org/acquisition",
            type = format.contentType,
            length = format.fileSize,
            mtime = mtime
        )
    }

    @Suppress("HttpUrlsUsage")
    private fun coverAtomLinks(book: Book): List<AtomLink> {
        val cover = book.id?.let { getBookCoverUseCase.getCoverIfExists(book.id)?.metadata }
        return cover?.let {
            listOf(mkCoverLink(book.id, "http://opds-spec.org/image", it),
                   mkCoverLink(book.id, "http://opds-spec.org/image/thumbnail", it))
        } ?: listOf()
    }

    private fun mkCoverLink(id: BookId, relSpec: String, metadata: BookCoverMetadata): AtomLink =
        AtomLink(href = "/api/books/${id.value}/cover",
                 rel = relSpec,
                 type = metadata.contentType)

    fun toEntry(author: Author): AtomEntry {
        val authorId = author.id?.value?.toString() ?: ""
        val links = if (author.id != null) {
            listOf(AtomLink(href = "/opds/v1.2/authors/${author.id.value}/books.xml",
                            rel = "subsection",
                            type = "application/atom+xml;profile=opds-catalog;kind=acquisition",
                            title = "Books by ${author.firstName} ${author.lastName}"))
        } else emptyList()

        return AtomEntry(id = "urn:uuid:$authorId",
                         title = "${author.firstName} ${author.lastName}",
                         updated = ZonedDateTime.now().format(dateFormatter), // Authors don't have updated field in domain?
                         links = links,
                         content = AtomContent(type = "text", text = "Books by ${author.firstName} ${author.lastName}"))
    }

    fun toEntry(series: Series): AtomEntry {
        val seriesId = series.id?.value?.toString() ?: ""
        val links = if (series.id != null) {
            listOf(AtomLink(href = "/opds/v1.2/series/${series.id.value}/books.xml",
                            rel = "subsection",
                            type = "application/atom+xml;profile=opds-catalog;kind=acquisition",
                            title = "Books in ${series.title}"))
        } else emptyList()

        return AtomEntry(id = "urn:uuid:$seriesId",
                         title = series.title,
                         updated = ZonedDateTime.now().format(dateFormatter),
                         links = links,
                         content = AtomContent(type = "text", text = "Books in series ${series.title}"))
    }
}
