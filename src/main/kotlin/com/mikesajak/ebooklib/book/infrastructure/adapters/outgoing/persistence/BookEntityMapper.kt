package com.mikesajak.ebooklib.book.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.author.infrastructure.adapters.outgoing.persistence.AuthorEntityMapper
import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.series.infrastructure.adapters.outgoing.persistence.SeriesEntityMapper
import org.springframework.stereotype.Component

@Component
class BookEntityMapper(
        private val authorEntityMapper: AuthorEntityMapper,
        private val seriesEntityMapper: SeriesEntityMapper
) {
    fun toEntity(book: Book): BookEntity =
        BookEntity(
                id = book.id?.value,
                title = book.title,
                authors = book.authors.map { authorEntityMapper.toEntity(it) }.toSet(),
                series = book.series?.let { seriesEntityMapper.toEntity(it) },
                volume = book.volume,
                creationDate = book.creationDate,
                publicationDate = book.publicationDate,
                publisher = book.publisher,
                description = book.description,
                labels = book.labels.toSet()
        )

    fun toDomain(bookEntity: BookEntity): Book =
        Book(
                id = bookEntity.id?.let { BookId(it) },
                title = bookEntity.title,
                authors = bookEntity.authors.map { authorEntityMapper.toDomain(it) },
                series = bookEntity.series?.let { seriesEntityMapper.toDomain(it) },
                volume = bookEntity.volume,
                creationDate = bookEntity.creationDate,
                publicationDate = bookEntity.publicationDate,
                publisher = bookEntity.publisher,
                description = bookEntity.description,
                labels = bookEntity.labels.toList()
        )
}
