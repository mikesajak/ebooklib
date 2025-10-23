package com.mikesajak.ebooklib.book.infrastructure.adapters.outgoing.persistence
import com.mikesajak.ebooklib.author.infrastructure.adapters.outgoing.persistence.AuthorEntityMapper

import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.domain.model.BookId
import org.springframework.stereotype.Component

@Component
class BookEntityMapper(
    private val authorEntityMapper: AuthorEntityMapper
) {
    fun toEntity(book: Book): BookEntity =
        BookEntity(
            id = book.id?.value,
            title = book.title,
            authors = book.authors.map { authorEntityMapper.toEntity(it) }.toSet(),
            creationDate = book.creationDate,
            publicationDate = book.publicationDate,
            publisher = book.publisher,
            description = book.description
        )

    fun toDomain(bookEntity: BookEntity): Book =
        Book(
            id = bookEntity.id?.let { BookId(it) },
            title = bookEntity.title,
            authors = bookEntity.authors.map { authorEntityMapper.toDomain(it) },
            creationDate = bookEntity.creationDate,
            publicationDate = bookEntity.publicationDate,
            publisher = bookEntity.publisher,
            description = bookEntity.description
        )
}
