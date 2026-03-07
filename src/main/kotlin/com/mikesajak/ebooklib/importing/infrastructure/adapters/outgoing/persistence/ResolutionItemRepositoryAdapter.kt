package com.mikesajak.ebooklib.importing.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.importing.application.ports.outgoing.ResolutionItemRepositoryPort
import com.mikesajak.ebooklib.importing.domain.model.ImportSessionId
import com.mikesajak.ebooklib.importing.domain.model.ResolutionItem
import com.mikesajak.ebooklib.importing.domain.model.ResolutionItemId
import org.springframework.stereotype.Component

@Component
class ResolutionItemRepositoryAdapter(
    private val jpaRepository: ResolutionItemJpaRepository,
    private val mapper: ResolutionItemEntityMapper
) : ResolutionItemRepositoryPort {

    override fun save(resolutionItem: ResolutionItem): ResolutionItem =
        mapper.toDomain(jpaRepository.save(mapper.toEntity(resolutionItem)))

    override fun findById(id: ResolutionItemId): ResolutionItem? =
        jpaRepository.findById(id.value).map { mapper.toDomain(it) }.orElse(null)

    override fun findByImportSessionIdAndTitleAndAuthors(
        importSessionId: ImportSessionId,
        title: String,
        authors: List<String>
    ): ResolutionItem? =
        jpaRepository.findByImportSessionIdAndTitleAndAuthors(
            importSessionId.value,
            title,
            authors.joinToString(", ")
        )?.let { mapper.toDomain(it) }

    override fun findByImportSessionId(importSessionId: ImportSessionId): List<ResolutionItem> =
        jpaRepository.findAllByImportSessionId(importSessionId.value).map { mapper.toDomain(it) }

    override fun delete(id: ResolutionItemId) {
        jpaRepository.deleteById(id.value)
    }

    override fun deleteByImportSessionId(importSessionId: ImportSessionId) {
        jpaRepository.deleteAllByImportSessionId(importSessionId.value)
    }
}
