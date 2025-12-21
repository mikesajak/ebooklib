package com.mikesajak.ebooklib.author.infrastructure.adapters.incoming.rest

import com.mikesajak.ebooklib.author.application.projection.AuthorProjection
import com.mikesajak.ebooklib.author.domain.model.Author
import com.mikesajak.ebooklib.author.infrastructure.adapters.incoming.rest.dto.AuthorRequestDto
import com.mikesajak.ebooklib.author.infrastructure.adapters.incoming.rest.dto.AuthorResponseDto
import org.springframework.stereotype.Component

@Component
class AuthorRestMapper {
    fun toResponse(author: Author) =
        AuthorResponseDto(author.id!!.value, author.firstName, author.lastName, author.bio, author.birthDate, author.deathDate, 0)

    fun toResponse(author: AuthorProjection) =
        AuthorResponseDto(author.id, author.firstName, author.lastName, author.bio, author.birthDate, author.deathDate, author.bookCount)

    fun toDomain(authorRequestDto: AuthorRequestDto): Author =
        Author(null, authorRequestDto.firstName, authorRequestDto.lastName, authorRequestDto.bio, authorRequestDto.birthDate, authorRequestDto.deathDate)
}