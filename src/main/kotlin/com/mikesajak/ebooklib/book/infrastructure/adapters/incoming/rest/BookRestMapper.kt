package com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest

import com.mikesajak.ebooklib.author.application.ports.incoming.GetAuthorUseCase
import com.mikesajak.ebooklib.author.domain.model.AuthorId
import com.mikesajak.ebooklib.author.infrastructure.adapters.incoming.rest.AuthorRestMapper
import com.mikesajak.ebooklib.book.application.ports.incoming.GetBookUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.ListEbookFormatsUseCase
import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.dto.BookRequestDto
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.dto.BookResponseDto
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.dto.EbookFormatFileDto
import com.mikesajak.ebooklib.series.application.ports.incoming.GetSeriesUseCase
import com.mikesajak.ebooklib.series.domain.model.SeriesId
import com.mikesajak.ebooklib.series.infrastructure.adapters.incoming.rest.SeriesRestMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class BookRestMapper(
        private val authorRestMapper: AuthorRestMapper,
        private val seriesRestMapper: SeriesRestMapper,

        @Autowired(required = false) private val getAuthorUseCase: GetAuthorUseCase? = null,
        @Autowired(required = false) private val getSeriesUseCase: GetSeriesUseCase? = null,
        @Autowired(required = false) private val listEbookFormatsUseCase: ListEbookFormatsUseCase? = null
) {
    fun toResponse(book: Book, view: BookView) =
        when (view) {
            BookView.FULL -> BookResponseDto(
                    id = book.id!!.value,
                    title = book.title,
                    authors = book.authors.map { authorRestMapper.toResponse(it) },
                    series = book.series?.let { seriesRestMapper.toResponse(it) },
                    volume = book.volume,
                    creationDate = book.creationDate,
                    publicationDate = book.publicationDate,
                    publisher = book.publisher,
                    description = book.description,
                    labels = book.labels,
                    formats = listEbookFormatsUseCase?.listFormatFiles(book.id)
                        ?.map {
                            EbookFormatFileDto(
                                id = it.id.toString(),
                                fileName = it.fileName,
                                contentType = it.contentType,
                                size = it.fileSize,
                                formatType = it.formatType
                            )
                        }
            )

            BookView.COMPACT -> BookResponseDto(
                    id = book.id!!.value,
                    title = book.title,
                    authors = book.authors.map { authorRestMapper.toResponse(it) },
                    series = book.series?.let { seriesRestMapper.toResponse(it) },
                    volume = book.volume,
                    creationDate = book.creationDate,
                    publicationDate = book.publicationDate,
                    publisher = book.publisher,
                    description = null,
                    labels = book.labels,
                    formats = listEbookFormatsUseCase?.listFormatFiles(book.id)
                        ?.map {
                            EbookFormatFileDto(
                                id = it.id.toString(),
                                fileName = it.fileName,
                                contentType = it.contentType,
                                size = it.fileSize,
                                formatType = it.formatType
                            )
                        }
            )

            BookView.BY_AUTHOR -> BookResponseDto(
                    id = book.id!!.value,
                    title = book.title,
                    authors = book.authors.map { authorRestMapper.toResponse(it) },
                    series = null,
                    volume = null,
                    creationDate = null,
                    publicationDate = null,
                    publisher = null,
                    description = null,
                    labels = emptyList()
            )

            BookView.BY_SERIES -> BookResponseDto(
                    id = book.id!!.value,
                    title = book.title,
                    authors = emptyList(),
                    series = book.series?.let { seriesRestMapper.toResponse(it) },
                    volume = book.volume,
                    creationDate = null,
                    publicationDate = null,
                    publisher = null,
                    description = null,
                    labels = emptyList()
            )
        }

    fun toDomain(bookRequestDto: BookRequestDto): Book {
        val authors = bookRequestDto.authorIds.map { authorId -> getAuthorUseCase?.getAuthor(AuthorId(authorId)) ?: error("GetAuthorUseCase not injected") }
        val series = bookRequestDto.seriesId?.let { seriesId -> getSeriesUseCase?.getSeries(SeriesId(seriesId)) }
        return Book(null,
                    bookRequestDto.title,
                    authors,
                    bookRequestDto.creationDate,
                    bookRequestDto.publicationDate,
                    bookRequestDto.publisher,
                    bookRequestDto.description,
                    series,
                    bookRequestDto.volume,
                    bookRequestDto.labels ?: emptyList())
    }
}
