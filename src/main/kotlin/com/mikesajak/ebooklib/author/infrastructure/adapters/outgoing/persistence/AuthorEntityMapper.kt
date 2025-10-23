package com.mikesajak.ebooklib.author.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.author.domain.model.Author
import com.mikesajak.ebooklib.author.domain.model.AuthorId
import org.springframework.stereotype.Component

@Component
class AuthorEntityMapper {

    fun toEntity(author: Author): AuthorEntity =
        AuthorEntity(
            id = author.id?.value,
            name = author.name,
            bio = author.bio,
            birthDate = author.birthDate,
            deathDate = author.deathDate
        )

    fun toDomain(authorEntity: AuthorEntity): Author =
        Author(
            id = authorEntity.id?.let { AuthorId(it) },
            name = authorEntity.name,
            bio = authorEntity.bio,
            birthDate = authorEntity.birthDate,
            deathDate = authorEntity.deathDate
        )
}
