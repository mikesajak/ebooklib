package com.mikesajak.ebooklib.common.domain.model

data class PaginatedResult<T>(
        val content: List<T>,
        val page: Int,
        val size: Int,
        val totalElements: Long,
        val totalPages: Int
) {
    val isEmpty: Boolean
        get() = content.isEmpty()

    val isNotEmpty: Boolean
        get() = content.isNotEmpty()

    val hasNext: Boolean
        get() = page + 1 < totalPages

    val hasPrevious: Boolean
        get() = page > 0

    val isFirst: Boolean
        get() = page == 0

    val isLast: Boolean
        get() = page + 1 >= totalPages

    fun <R> map(transform: (T) -> R): PaginatedResult<R> =
        PaginatedResult(
            content = content.map(transform),
            page = page,
            size = size,
            totalElements = totalElements,
            totalPages = totalPages
        )

    companion object {
        fun <T> empty(page: Int = 0, size: Int = PaginationRequest.DEFAULT_PAGE_SIZE): PaginatedResult<T> =
            PaginatedResult(
                    content = emptyList(),
                    page = page,
                    size = size,
                    totalElements = 0,
                    totalPages = 0
            )
    }
}