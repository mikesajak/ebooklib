package com.mikesajak.ebooklib.author.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.author.application.projection.AuthorProjection
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import java.util.*

interface AuthorJpaRepository
    : JpaRepository<AuthorEntity, UUID>,
      JpaSpecificationExecutor<AuthorEntity>,
      AuthorCustomRepository {

    @Query(
        value = "SELECT new com.mikesajak.ebooklib.author.application.projection.AuthorProjection(a.id, a.firstName, a.lastName, a.bio, a.birthDate, a.deathDate, " +
                "(SELECT count(b) FROM BookEntity b WHERE a MEMBER OF b.authors)) " +
                "FROM AuthorEntity a",
        countQuery = "SELECT count(a) FROM AuthorEntity a"
    )

    fun findAuthorsWithBookCount(pageable: Pageable): Page<AuthorProjection>
}