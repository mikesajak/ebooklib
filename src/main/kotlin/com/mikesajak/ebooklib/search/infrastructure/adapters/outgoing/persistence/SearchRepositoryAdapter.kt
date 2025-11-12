package com.mikesajak.ebooklib.search.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.infrastructure.adapters.outgoing.persistence.BookEntity
import com.mikesajak.ebooklib.book.infrastructure.adapters.outgoing.persistence.BookEntityMapper
import com.mikesajak.ebooklib.book.infrastructure.adapters.outgoing.persistence.BookJpaRepository
import com.mikesajak.ebooklib.search.application.ports.outgoing.SearchRepositoryPort
import com.mikesajak.ebooklib.search.infrastructure.adapters.outgoing.persistence.rsql.RSQLSpecParser
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class SearchRepositoryAdapter(
        private val rsqlParser: RSQLSpecParser,
        private val bookJpaRepository: BookJpaRepository,
        private val mapper: BookEntityMapper
) : SearchRepositoryPort {
    override fun search(query: String, pageable: Pageable): Page<Book> {
        return searchEntity(query, pageable)
                .map { mapper.toDomain(it!!) }
    }

    private fun searchEntity(query: String, pageable: Pageable): Page<BookEntity?> {
        val querySpec = rsqlParser.parseOrNull<BookEntity>(query)

        return if (querySpec == null) bookJpaRepository.findAll(pageable)
        else bookJpaRepository.findAll(querySpec, pageable)

    }
}