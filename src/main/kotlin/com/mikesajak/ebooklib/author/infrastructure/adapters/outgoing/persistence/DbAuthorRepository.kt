package com.mikesajak.ebooklib.author.infrastructure.adapters.outgoing.persistence
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

import com.mikesajak.ebooklib.author.application.ports.outgoing.AuthorRepositoryPort
import com.mikesajak.ebooklib.author.domain.model.Author
import com.mikesajak.ebooklib.author.domain.model.AuthorId
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Repository
import java.util.*

@Repository
@Primary
class DbAuthorRepository(
    private val authorJpaRepository: AuthorJpaRepository,
    private val mapper: AuthorEntityMapper
) : AuthorRepositoryPort {

    override fun findAll(pageable: Pageable): Page<Author> =
        authorJpaRepository.findAll(pageable).map { mapper.toDomain(it) }

    override fun findById(id: AuthorId): Author? =
        authorJpaRepository.findById(id.value).map { mapper.toDomain(it) }.orElse(null)

    override fun save(author: Author): Author {
        val entity = mapper.toEntity(author)
        val entityToSave = if (entity.id == null) entity.copy(id = UUID.randomUUID()) else entity
        val savedEntity = authorJpaRepository.save(entityToSave)
        return mapper.toDomain(savedEntity)
    }
}