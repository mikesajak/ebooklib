package com.mikesajak.ebooklib.search.infrastructure.adapters.incoming.rest

import com.mikesajak.ebooklib.author.infrastructure.adapters.incoming.rest.AuthorRestMapper
import com.mikesajak.ebooklib.author.infrastructure.adapters.incoming.rest.dto.AuthorResponseDto
import com.mikesajak.ebooklib.infrastructure.incoming.rest.dto.PageResponse
import com.mikesajak.ebooklib.infrastructure.incoming.rest.toPageResponse
import com.mikesajak.ebooklib.infrastructure.web.toDomainPagination
import com.mikesajak.ebooklib.search.application.ports.incoming.SearchAuthorsUseCase
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/authors/search")
class SearchAuthorsController(
        private val searchAuthorsUseCase: SearchAuthorsUseCase,
        private val authorRestMapper: AuthorRestMapper
) {
    @GetMapping
    fun search(
            @RequestParam(required = false) query: String?,
            @PageableDefault(size = 10) pageable: Pageable
    ): PageResponse<AuthorResponseDto> =
        searchAuthorsUseCase.search(query ?: "", pageable.toDomainPagination())
                .toPageResponse { author -> authorRestMapper.toResponse(author) }
}
