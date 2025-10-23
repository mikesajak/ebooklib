package com.mikesajak.ebooklib.author.domain.model

import java.time.LocalDate
import java.util.UUID

data class AuthorId(val value: UUID)

data class Author(
    val id: AuthorId?,
    val name: String,
    val bio: String?,
    val birthDate: LocalDate?,
    val deathDate: LocalDate?
)