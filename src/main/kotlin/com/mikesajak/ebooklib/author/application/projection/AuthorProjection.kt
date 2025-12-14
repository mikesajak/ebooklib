package com.mikesajak.ebooklib.author.application.projection

import java.time.LocalDate
import java.util.UUID

data class AuthorProjection(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val bio: String?,
    val birthDate: LocalDate?,
    val deathDate: LocalDate?,
    val bookCount: Long
)
