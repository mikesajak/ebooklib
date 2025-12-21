package com.mikesajak.ebooklib.author.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.author.domain.model.Author
import com.mikesajak.ebooklib.author.domain.model.AuthorId
import com.mikesajak.ebooklib.author.application.projection.AuthorProjection
import org.springframework.stereotype.Component

@Component
class AuthorEntityMapper {

    fun toEntity(author: Author): AuthorEntity =
        AuthorEntity(
            id = author.id?.value,
            firstName = author.firstName,
            lastName = author.lastName,
            bio = author.bio,
            birthDate = author.birthDate,
            deathDate = author.deathDate
        )

    fun toDomain(authorEntity: AuthorEntity): Author =
        Author(
            id = authorEntity.id?.let { AuthorId(it) },
            firstName = authorEntity.firstName,
            lastName = authorEntity.lastName,
            bio = authorEntity.bio,
            birthDate = authorEntity.birthDate,
            deathDate = authorEntity.deathDate
        )
}