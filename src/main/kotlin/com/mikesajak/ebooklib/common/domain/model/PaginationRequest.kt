package com.mikesajak.ebooklib.common.domain.model

data class PaginationRequest(
        val page: Int = 0,
        val size: Int = 20,
        val sort: List<SortOrder> = emptyList()
) {
    init {
        require(page >= 0) { "Page number must be >= 0" }
        require(size > 0) { "Page size must be > 0" }
        require(size <= MAX_PAGE_SIZE || size == Int.MAX_VALUE) { "Page size must be <= $MAX_PAGE_SIZE or Int.MAX_VALUE" }
    }

    companion object {
        const val MAX_PAGE_SIZE = 1000
        const val DEFAULT_PAGE_SIZE = 20

        fun unpaged(): PaginationRequest = PaginationRequest(page = 0, size = Int.MAX_VALUE)
    }
}

