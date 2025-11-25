package com.mikesajak.ebooklib.opds.infrastructure.adapters.incoming.rest

import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.opds.infrastructure.adapters.incoming.rest.dto.Contributor
import com.mikesajak.ebooklib.opds.infrastructure.adapters.incoming.rest.dto.Link
import com.mikesajak.ebooklib.opds.infrastructure.adapters.incoming.rest.dto.Publication
import com.mikesajak.ebooklib.opds.infrastructure.adapters.incoming.rest.dto.PublicationMetadata
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter

@Component
class OpdsBookMapper {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun toPublication(book: Book): Publication {
        return Publication(
                metadata = toPublicationMetadata(book),
                links = listOf(
                        createSelfLink(book.id!!)
                )
        )
    }

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
