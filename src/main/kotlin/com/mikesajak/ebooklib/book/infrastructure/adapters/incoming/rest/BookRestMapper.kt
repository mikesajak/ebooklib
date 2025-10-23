package com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest

import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.dto.BookRequestDto
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.dto.BookResponseDto
import org.springframework.stereotype.Component

@Component
class BookRestMapper {
    fun toResponse(book: Book) =
        BookResponseDto(book.id!!.value, book.title, book.author, book.creationDate, book.publicationDate, book.publisher, book.description)

    fun toDomain(bookRequestDto: BookRequestDto): Book =
        Book(null, bookRequestDto.title, bookRequestDto.author, bookRequestDto.creationDate, bookRequestDto.publicationDate, bookRequestDto.publisher, bookRequestDto.description)
}
