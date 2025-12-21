package com.mikesajak.ebooklib.search.application.ports.incoming

import com.mikesajak.ebooklib.author.domain.model.Author
import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.common.domain.model.PaginationRequest

interface SearchAuthorsUseCase {
    fun search(query: String, pagination: PaginationRequest): PaginatedResult<Author>
}
