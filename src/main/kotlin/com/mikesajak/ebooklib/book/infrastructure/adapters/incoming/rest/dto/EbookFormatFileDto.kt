package com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.dto

data class EbookFormatFileDto(
    val id: String,
    val fileName: String,
    val contentType: String,
    val size: Long,
    val formatType: String
)