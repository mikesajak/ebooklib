package com.mikesajak.ebooklib.importing.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.importing.domain.model.ImportSessionStatus
import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "import_session")
class ImportSessionEntity(
    @Id
    val id: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: ImportSessionStatus,

    @Column(nullable = false)
    val totalFiles: Int,

    @Column(nullable = false)
    val processedFiles: Int,

    @Column(nullable = false)
    val failedFiles: Int,

    @Column(nullable = false)
    val createdAt: Instant,

    @Column(nullable = false)
    val updatedAt: Instant,

    @Column(nullable = false)
    val expiryAt: Instant
)
