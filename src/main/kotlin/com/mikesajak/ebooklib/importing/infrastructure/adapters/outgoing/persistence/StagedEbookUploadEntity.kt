package com.mikesajak.ebooklib.importing.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadStatus
import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "staged_ebook_upload")
class StagedEbookUploadEntity(
    @Id
    val id: UUID,

    @Column(nullable = false)
    val fileName: String,

    @Column(nullable = false)
    val contentType: String,

    @Column(nullable = false)
    val fileSize: Long,

    @Column(columnDefinition = "TEXT")
    val metadataJson: String?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: StagedEbookUploadStatus,

    @Column(nullable = false)
    val createdAt: Instant,

    @Column(nullable = false)
    val expiryAt: Instant
)
