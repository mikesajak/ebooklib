package com.mikesajak.ebooklib.infrastructure.incoming.rest.dto

data class PageResponse<T>(
        val content: List<T>,
        val page: PageMetadata
)

data class PageMetadata(
        val number: Int,
        val size: Int,
        val totalElements: Long,
        val totalPages: Int,
        val first: Boolean,
        val last: Boolean,
        val hasNext: Boolean,
        val hasPrevious: Boolean
)