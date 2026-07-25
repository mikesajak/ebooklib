package com.mikesajak.ebooklib.importing.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.importing.domain.model.ResolutionItemStatus
import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "resolution_item")
class ResolutionItemEntity(
    @Id
    val id: UUID,

    @Column(name = "import_session_id", nullable = false)
    val importSessionId: UUID,

    @Column(nullable = false, columnDefinition = "TEXT")
    val title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val authors: String, // Comma separated or JSON? Let's use comma separated for now.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: ResolutionItemStatus,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    val metadataJson: String?
)
