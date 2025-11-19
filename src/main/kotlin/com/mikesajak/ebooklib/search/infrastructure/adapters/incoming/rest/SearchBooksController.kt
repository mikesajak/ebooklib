package com.mikesajak.ebooklib.search.infrastructure.adapters.incoming.rest

import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.BookRestMapper
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.BookView
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.dto.BookResponseDto
import com.mikesajak.ebooklib.infrastructure.incoming.rest.dto.PageResponse
import com.mikesajak.ebooklib.infrastructure.incoming.rest.toPageResponse
import com.mikesajak.ebooklib.infrastructure.web.toDomainPagination
import com.mikesajak.ebooklib.search.application.ports.incoming.SearchByRSQLUseCase
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/books/search")
class SearchBooksController(
        private val searchByRSQLUseCase: SearchByRSQLUseCase,
        private val bookRestMapper: BookRestMapper
) {
    @GetMapping
    fun search(
            @RequestParam(required = false) query: String,
            @PageableDefault(size = 10) pageable: Pageable,
            @RequestParam(name = "view", required = false, defaultValue = "COMPACT") view: BookView
    ): PageResponse<BookResponseDto> =
        searchByRSQLUseCase.search(query, pageable.toDomainPagination())
                .toPageResponse { book -> bookRestMapper.toResponse(book, view) }
}