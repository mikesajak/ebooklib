package com.mikesajak.ebooklib.author.infrastructure.adapters.outgoing.persistence

import jakarta.persistence.*
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "authors")
data class AuthorEntity(
    @Id
    val id: UUID? = null,
    @Column(columnDefinition = "TEXT", nullable = false)
    val firstName: String,
    @Column(columnDefinition = "TEXT", nullable = false)
    val lastName: String,
    @Column(columnDefinition = "TEXT", nullable = true)
    val bio: String?,
    val birthDate: LocalDate?,
    val deathDate: LocalDate?,
)
