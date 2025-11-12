package com.mikesajak.ebooklib.search.application.services

import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.search.application.ports.incoming.SearchByRSQLUseCase
import com.mikesajak.ebooklib.search.application.ports.outgoing.SearchRepositoryPort
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SearchService(
        private val searchRepositoryPort: SearchRepositoryPort
) : SearchByRSQLUseCase {

    @Transactional(readOnly = true)
    override fun search(query: String, pageable: Pageable): Page<Book> {
        return searchRepositoryPort.search(query, pageable)
    }

}