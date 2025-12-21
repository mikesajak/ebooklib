package com.mikesajak.ebooklib.search.infrastructure.adapters.incoming.rest

import com.mikesajak.ebooklib.series.infrastructure.adapters.incoming.rest.SeriesRestMapper
import com.mikesajak.ebooklib.series.infrastructure.adapters.incoming.rest.dto.SeriesResponseDto
import com.mikesajak.ebooklib.infrastructure.incoming.rest.dto.PageResponse
import com.mikesajak.ebooklib.infrastructure.incoming.rest.toPageResponse
import com.mikesajak.ebooklib.infrastructure.web.toDomainPagination
import com.mikesajak.ebooklib.search.application.ports.incoming.SearchSeriesUseCase
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/series/search")
class SearchSeriesController(
        private val searchSeriesUseCase: SearchSeriesUseCase,
        private val seriesRestMapper: SeriesRestMapper
) {
    @GetMapping
    fun search(
            @RequestParam(required = false) query: String?,
            @PageableDefault(size = 10) pageable: Pageable
    ): PageResponse<SeriesResponseDto> =
        searchSeriesUseCase.search(query ?: "", pageable.toDomainPagination())
                .toPageResponse { series -> seriesRestMapper.toResponse(series) }
}
