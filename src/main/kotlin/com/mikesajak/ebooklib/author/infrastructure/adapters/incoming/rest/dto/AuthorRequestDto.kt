package com.mikesajak.ebooklib.author.infrastructure.adapters.incoming.rest.dto

import java.time.LocalDate

data class AuthorRequestDto(
    val name: String,
    val bio: String?,
    val birthDate: LocalDate?,
    val deathDate: LocalDate?
)