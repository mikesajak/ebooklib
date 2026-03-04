package com.mikesajak.ebooklib.importing.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.importing.domain.model.ImportSession
import com.mikesajak.ebooklib.importing.domain.model.ImportSessionId
import org.springframework.stereotype.Component

@Component
class ImportSessionEntityMapper {
    fun toEntity(domain: ImportSession): ImportSessionEntity =
        ImportSessionEntity(
            id = domain.id.value,
            status = domain.status,
            totalFiles = domain.totalFiles,
            processedFiles = domain.processedFiles,
            failedFiles = domain.failedFiles,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
            expiryAt = domain.expiryAt
        )

    fun toDomain(entity: ImportSessionEntity): ImportSession =
        ImportSession(
            id = ImportSessionId(entity.id),
            status = entity.status,
            totalFiles = entity.totalFiles,
            processedFiles = entity.processedFiles,
            failedFiles = entity.failedFiles,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            expiryAt = entity.expiryAt
        )
}
