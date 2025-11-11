package com.mikesajak.ebooklib.series.infrastructure.adapters.outgoing.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "series")
data class SeriesEntity(
    @Id
    val id: UUID? = null,
    @Column(columnDefinition = "TEXT", nullable = false)
    val title: String,
    @Column(columnDefinition = "TEXT", nullable = true)
    val description: String?
)