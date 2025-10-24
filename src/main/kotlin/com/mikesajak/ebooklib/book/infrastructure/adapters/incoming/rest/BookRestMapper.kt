package com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest

import com.mikesajak.ebooklib.author.application.ports.incoming.GetAuthorUseCase
import com.mikesajak.ebooklib.author.domain.model.AuthorId
import com.mikesajak.ebooklib.author.infrastructure.adapters.incoming.rest.AuthorRestMapper
import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.dto.BookRequestDto
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.dto.BookResponseDto
import com.mikesajak.ebooklib.series.application.ports.incoming.GetSeriesUseCase
import com.mikesajak.ebooklib.series.domain.model.SeriesId
import com.mikesajak.ebooklib.series.infrastructure.adapters.incoming.rest.SeriesRestMapper
import org.springframework.stereotype.Component

@Component
class BookRestMapper(
    private val authorRestMapper: AuthorRestMapper,
    private val seriesRestMapper: SeriesRestMapper,

    private val getAuthorUseCase: GetAuthorUseCase,
    private val getSeriesUseCase: GetSeriesUseCase
) {
    fun toResponse(book: Book) =
        BookResponseDto(book.id!!.value,
                        book.title,
                        book.authors.map { authorRestMapper.toResponse(it) },
                        book.series?.let { seriesRestMapper.toResponse(it) },
                        book.volume,
                        book.creationDate,
                        book.publicationDate,
                        book.publisher,
                        book.description)

    fun toDomain(bookRequestDto: BookRequestDto): Book {
        val authors = bookRequestDto.authorIds.map { authorId -> getAuthorUseCase.getAuthor(AuthorId(authorId)) }
        val series = bookRequestDto.seriesId?.let { seriesId -> getSeriesUseCase.getSeries(SeriesId(seriesId)) }
        return Book(null,
                    bookRequestDto.title,
                    authors,
                    bookRequestDto.creationDate,
                    bookRequestDto.publicationDate,
                    bookRequestDto.publisher,
                    bookRequestDto.description,
                    series,
                    bookRequestDto.volume)
    }
}
