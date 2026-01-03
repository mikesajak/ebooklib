package com.mikesajak.ebooklib.opds.infrastructure.adapters.incoming.rest

import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.domain.model.BookCover
import com.mikesajak.ebooklib.book.domain.model.BookCoverMetadata
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.book.domain.model.EbookFormatFile
import com.mikesajak.ebooklib.opds.infrastructure.adapters.incoming.rest.dto.Contributor
import com.mikesajak.ebooklib.opds.infrastructure.adapters.incoming.rest.dto.Link
import com.mikesajak.ebooklib.opds.infrastructure.adapters.incoming.rest.dto.Publication
import com.mikesajak.ebooklib.opds.infrastructure.adapters.incoming.rest.dto.PublicationMetadata
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter

@Component
class OpdsBookMapper {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun toSummaryPublication(book: Book, cover: BookCoverMetadata?, formats: List<EbookFormatFile>): Publication {
        val images = cover?.let { coverMetadata ->
            listOf(
                Link(
                    rel = "http://opds-spec.org/image",
                    href = "/api/books/${book.id!!.value}/cover",
                    type = coverMetadata.contentType
                )
            )
        }

        val acquisitionLinks = formats.map { format ->
            Link(
                rel = "http://opds-spec.org/acquisition",
                href = "/api/books/${book.id!!.value}/formats/${format.id}/download",
                type = format.contentType
            )
        }

        val links = mutableListOf(createSelfLink(book.id!!))
        links.addAll(acquisitionLinks)

        return Publication(
            metadata = toPublicationMetadata(book),
            links = links,
            images = images
        )
    }

    fun toFullPublication(book: Book, cover: BookCoverMetadata?, formats: List<EbookFormatFile>): Publication {
        return toSummaryPublication(book, cover, formats)
    }

    fun toPublication(book: Book, cover: BookCoverMetadata?, formats: List<EbookFormatFile>): Publication =
        toFullPublication(book, cover, formats)

    private fun toPublicationMetadata(book: Book): PublicationMetadata {
        return PublicationMetadata(
                title = book.title,
                author = book.authors.map { Contributor(name = "${it.firstName} ${it.lastName}") },
                published = book.publicationDate?.format(dateFormatter),
                modified = book.creationDate?.format(dateFormatter),
                description = book.description,
                identifier = book.id!!.value.toString() // Using book ID as identifier for now
        )
    }

    private fun createSelfLink(bookId: BookId): Link {
        // This link will point to a future endpoint for the book's detailed OPDS entry
        // We will use a placeholder URL for now.
        return Link(
                href = "/opds/v2/books/${bookId.value}.json",
                type = "application/opds+json",
                rel = "self"
        )
    }
}
