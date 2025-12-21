package com.mikesajak.ebooklib.search.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.author.domain.model.Author
import com.mikesajak.ebooklib.author.infrastructure.adapters.outgoing.persistence.AuthorEntity
import com.mikesajak.ebooklib.author.infrastructure.adapters.outgoing.persistence.AuthorEntityMapper
import com.mikesajak.ebooklib.author.infrastructure.adapters.outgoing.persistence.AuthorJpaRepository
import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.common.domain.model.PaginationRequest
import com.mikesajak.ebooklib.infrastructure.web.toDomainPage
import com.mikesajak.ebooklib.infrastructure.web.toSpringPageable
import com.mikesajak.ebooklib.search.application.ports.outgoing.SearchAuthorsRepositoryPort
import com.mikesajak.ebooklib.search.infrastructure.adapters.outgoing.persistence.rsql.AuthorSearchFieldMapper
import com.mikesajak.ebooklib.search.infrastructure.adapters.outgoing.persistence.rsql.RSQLSpecParser
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class SearchAuthorsRepositoryAdapter(
        private val rsqlParser: RSQLSpecParser,
        private val authorJpaRepository: AuthorJpaRepository,
        private val mapper: AuthorEntityMapper,
        private val searchFieldMapper: AuthorSearchFieldMapper
) : SearchAuthorsRepositoryPort {
    override fun search(query: String, pagination: PaginationRequest): PaginatedResult<com.mikesajak.ebooklib.author.application.projection.AuthorProjection> {
        return searchProjection(query, pagination.toSpringPageable())
                .toDomainPage { it }
    }

    private fun searchProjection(query: String, pageable: Pageable): Page<com.mikesajak.ebooklib.author.application.projection.AuthorProjection> {
        val querySpec = rsqlParser.parseOrNull<AuthorEntity>(query, searchFieldMapper)

        return authorJpaRepository.findAuthorsWithBookCount(querySpec, pageable)
    }
}
