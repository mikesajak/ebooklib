package com.mikesajak.ebooklib.common.domain.model

data class SortOrder(
        val property: String,
        val direction: SortDirection = SortDirection.ASC
)