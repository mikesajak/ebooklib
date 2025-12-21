package com.mikesajak.ebooklib.search.application.ports.outgoing

import com.mikesajak.ebooklib.author.application.projection.AuthorProjection
import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.common.domain.model.PaginationRequest

interface SearchAuthorsRepositoryPort {
    fun search(query: String, pagination: PaginationRequest): PaginatedResult<AuthorProjection>
}
