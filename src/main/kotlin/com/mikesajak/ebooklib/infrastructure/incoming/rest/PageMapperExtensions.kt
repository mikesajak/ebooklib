package com.mikesajak.ebooklib.infrastructure.incoming.rest

import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.infrastructure.incoming.rest.dto.PageMetadata
import com.mikesajak.ebooklib.infrastructure.incoming.rest.dto.PageResponse

fun <T, R> PaginatedResult<T>.toPageResponse(mapper: (T) -> R): PageResponse<R> {
    return PageResponse(
            content = content.map(mapper),
            page = PageMetadata(
                    number = page,
                    size = size,
                    totalElements = totalElements,
                    totalPages = totalPages,
                    first = isFirst,
                    last = isLast,
                    hasNext = hasNext,
                    hasPrevious = hasPrevious
            )
    )
}

fun <T> PaginatedResult<T>.toPageResponse(): PageResponse<T> = toPageResponse { it }