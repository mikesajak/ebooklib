package com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.dto

import java.time.LocalDate
import java.util.*

data class BookResponseDto(val id: UUID,
                           val title: String,
                           val author: String,
                           val creationDate: LocalDate?,
                           val publicationDate: LocalDate?,
                           val publisher: String?,
                           val description: String?)
