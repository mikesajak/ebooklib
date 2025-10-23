package com.mikesajak.ebooklib.author.infrastructure.adapters.incoming.rest.dto

import java.time.LocalDate
import java.util.*

data class AuthorResponseDto(
    val id: UUID,
    val name: String,
    val bio: String?,
    val birthDate: LocalDate?,
    val deathDate: LocalDate?
)