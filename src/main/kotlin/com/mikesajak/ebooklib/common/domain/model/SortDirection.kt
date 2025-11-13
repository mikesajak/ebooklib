package com.mikesajak.ebooklib.common.domain.model

enum class SortDirection {
    ASC, DESC;

    fun isAscending(): Boolean = this == ASC;
    fun isDescending(): Boolean = this == DESC;
}