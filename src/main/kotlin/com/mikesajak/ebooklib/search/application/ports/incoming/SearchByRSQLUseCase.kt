package com.mikesajak.ebooklib.search.application.ports.incoming

import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.common.domain.model.PaginationRequest

interface SearchByRSQLUseCase {
    fun search(query: String, pagination: PaginationRequest): PaginatedResult<Book>
}