package com.mikesajak.ebooklib.importing.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.importing.application.ports.outgoing.ImportSessionRepositoryPort
import com.mikesajak.ebooklib.importing.domain.model.ImportSession
import com.mikesajak.ebooklib.importing.domain.model.ImportSessionId
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ImportSessionRepositoryAdapter(
    private val repository: ImportSessionJpaRepository,
    private val mapper: ImportSessionEntityMapper
) : ImportSessionRepositoryPort {

    override fun save(importSession: ImportSession): ImportSession =
        mapper.toDomain(repository.save(mapper.toEntity(importSession)))

    override fun findById(id: ImportSessionId): ImportSession? =
        repository.findByIdOrNull(id.value)?.let { mapper.toDomain(it) }

    override fun findAllByStatus(status: com.mikesajak.ebooklib.importing.domain.model.ImportSessionStatus): List<ImportSession> =
        repository.findAllByStatus(status).map { mapper.toDomain(it) }

    override fun findAllExpired(now: Instant): List<ImportSession> =
        repository.findAllByExpiryAtBefore(now).map { mapper.toDomain(it) }

    override fun delete(id: ImportSessionId) {
        repository.deleteById(id.value)
    }

    override fun incrementProcessed(id: ImportSessionId) {
        repository.incrementProcessed(id.value, Instant.now())
    }

    override fun incrementFailed(id: ImportSessionId) {
        repository.incrementFailed(id.value, Instant.now())
    }
}
