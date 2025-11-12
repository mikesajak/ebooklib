package com.mikesajak.ebooklib.search.infrastructure.adapters.incoming.rest

import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.BookRestMapper
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.dto.BookResponseDto
import com.mikesajak.ebooklib.search.application.ports.incoming.SearchByRSQLUseCase
import org.springframework.data.domain.Page
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
            @PageableDefault(size = 10) pageable: Pageable
    ): Page<BookResponseDto> =
        searchByRSQLUseCase.search(query, pageable)
                .map { book -> bookRestMapper.toResponse(book) }
}