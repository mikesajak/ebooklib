package com.mikesajak.ebooklib.search.application.ports.outgoing

import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.common.domain.model.PaginationRequest

interface SearchRepositoryPort {
    fun search(query: String, pagination: PaginationRequest): PaginatedResult<Book>
}