package com.mikesajak.ebooklib.series.infrastructure.adapters.outgoing.persistence

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "series")
data class SeriesEntity(
    @Id
    val id: UUID? = null,
    val title: String,
    val description: String?
)