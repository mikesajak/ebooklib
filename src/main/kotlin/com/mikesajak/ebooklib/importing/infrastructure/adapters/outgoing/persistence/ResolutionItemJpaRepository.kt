package com.mikesajak.ebooklib.importing.infrastructure.adapters.outgoing.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ResolutionItemJpaRepository : JpaRepository<ResolutionItemEntity, UUID> {
    fun findByImportSessionIdAndTitleAndAuthors(importSessionId: UUID, title: String, authors: String): ResolutionItemEntity?
    fun findAllByImportSessionId(importSessionId: UUID): List<ResolutionItemEntity>
}
