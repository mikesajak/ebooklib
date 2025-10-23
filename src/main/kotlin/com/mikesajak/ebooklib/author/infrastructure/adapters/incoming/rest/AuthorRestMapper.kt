package com.mikesajak.ebooklib.author.infrastructure.adapters.incoming.rest

import com.mikesajak.ebooklib.author.domain.model.Author
import com.mikesajak.ebooklib.author.infrastructure.adapters.incoming.rest.dto.AuthorRequestDto
import com.mikesajak.ebooklib.author.infrastructure.adapters.incoming.rest.dto.AuthorResponseDto
import org.springframework.stereotype.Component

@Component
class AuthorRestMapper {
    fun toResponse(author: Author) =
        AuthorResponseDto(author.id!!.value, author.name, author.bio, author.birthDate, author.deathDate)

    fun toDomain(authorRequestDto: AuthorRequestDto): Author =
        Author(null, authorRequestDto.name, authorRequestDto.bio, authorRequestDto.birthDate, authorRequestDto.deathDate)
}