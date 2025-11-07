package com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.dto

import com.mikesajak.ebooklib.author.infrastructure.adapters.incoming.rest.dto.AuthorResponseDto
import com.mikesajak.ebooklib.series.infrastructure.adapters.incoming.rest.dto.SeriesResponseDto

import java.time.LocalDate
import java.util.*

data class BookResponseDto(val id: UUID,
                           val title: String,
                           val authors: List<AuthorResponseDto>,
                           val series: SeriesResponseDto?,
                           val volume: Int?,
                           val creationDate: LocalDate?,
                           val publicationDate: LocalDate?,
                           val publisher: String?,
                           val description: String?,
                           val labels: List<String>
)
