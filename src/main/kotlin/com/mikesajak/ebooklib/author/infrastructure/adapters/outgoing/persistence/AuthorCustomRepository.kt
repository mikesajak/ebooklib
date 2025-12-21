package com.mikesajak.ebooklib.author.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.author.application.projection.AuthorProjection
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification

interface AuthorCustomRepository {
    fun findAuthorsWithBookCount(spec: Specification<AuthorEntity>?, pageable: Pageable): Page<AuthorProjection>
}
