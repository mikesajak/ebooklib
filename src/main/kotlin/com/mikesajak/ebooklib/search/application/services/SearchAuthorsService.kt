package com.mikesajak.ebooklib.search.application.services

import com.mikesajak.ebooklib.author.domain.model.Author
import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.common.domain.model.PaginationRequest
import com.mikesajak.ebooklib.search.application.ports.incoming.SearchAuthorsUseCase
import com.mikesajak.ebooklib.search.application.ports.outgoing.SearchAuthorsRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SearchAuthorsService(
        private val searchAuthorsRepositoryPort: SearchAuthorsRepositoryPort
) : SearchAuthorsUseCase {

    @Transactional(readOnly = true)
    override fun search(query: String, pagination: PaginationRequest): PaginatedResult<Author> {
        return searchAuthorsRepositoryPort.search(query, pagination)
    }
}
