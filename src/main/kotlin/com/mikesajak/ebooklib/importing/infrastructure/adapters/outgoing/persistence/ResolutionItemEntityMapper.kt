package com.mikesajak.ebooklib.importing.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.importing.domain.model.ImportSessionId
import com.mikesajak.ebooklib.importing.domain.model.ResolutionItem
import com.mikesajak.ebooklib.importing.domain.model.ResolutionItemId
import org.springframework.stereotype.Component

@Component
class ResolutionItemEntityMapper {
    fun toEntity(domain: ResolutionItem): ResolutionItemEntity =
        ResolutionItemEntity(
            id = domain.id.value,
            importSessionId = domain.importSessionId.value,
            title = domain.title,
            authors = domain.authors.joinToString(", "),
            status = domain.status,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
            metadataJson = domain.metadataJson
        )

    fun toDomain(entity: ResolutionItemEntity): ResolutionItem =
        ResolutionItem(
            id = ResolutionItemId(entity.id),
            importSessionId = ImportSessionId(entity.importSessionId),
            title = entity.title,
            authors = entity.authors.split(", ").filter { it.isNotBlank() },
            status = entity.status,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            metadataJson = entity.metadataJson
        )
}
