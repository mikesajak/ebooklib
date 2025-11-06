package com.mikesajak.ebooklib.author.infrastructure.adapters.outgoing.persistence

import jakarta.persistence.*
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "authors")
data class AuthorEntity(
    @Id
    val id: UUID? = null,
    val firstName: String,
    val lastName: String,
    val bio: String?,
    val birthDate: LocalDate?,
    val deathDate: LocalDate?,
)