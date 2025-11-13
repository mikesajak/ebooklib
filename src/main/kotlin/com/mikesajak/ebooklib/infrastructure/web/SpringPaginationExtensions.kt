package com.mikesajak.ebooklib.infrastructure.web

import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.common.domain.model.PaginationRequest
import com.mikesajak.ebooklib.common.domain.model.SortDirection
import com.mikesajak.ebooklib.common.domain.model.SortOrder
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

fun Pageable.toDomainPagination(): PaginationRequest {
    val sortOrders = if (sort.isSorted) {
        sort.map { order ->
            SortOrder(
                    property = order.property,
                    direction = when (order.direction) {
                        Sort.Direction.ASC -> SortDirection.ASC
                        Sort.Direction.DESC -> SortDirection.DESC
                    }
            )
        }.toList()
    } else emptyList()

    return PaginationRequest(
            page = pageNumber,
            size = pageSize,
            sort = sortOrders
    )
}

fun PaginationRequest.toSpringPageable(): Pageable {
    val springSort = if (sort.isNotEmpty()) {
        Sort.by(sort.map { order ->
            Sort.Order(
                    when (order.direction) {
                        SortDirection.ASC -> Sort.Direction.ASC
                        SortDirection.DESC -> Sort.Direction.DESC
                    },
                    order.property
            )
        })
    } else Sort.unsorted()

    return PageRequest.of(page, size, springSort)
}

fun <T, R> Page<T>.toDomainPage(mapper: (T) -> R): PaginatedResult<R> {
    return PaginatedResult(
            content = content.map(mapper),
            page = number,
            size = size,
            totalElements = totalElements,
            totalPages = totalPages,
    )
}

fun <T> Page<T>.toDomainPage(): PaginatedResult<T> = toDomainPage { it }
