package com.mikesajak.ebooklib.importing.infrastructure.adapters.outgoing.persistence

import com.fasterxml.jackson.databind.ObjectMapper
import com.mikesajak.ebooklib.importing.application.ports.outgoing.StagedEbookUploadRepositoryPort
import com.mikesajak.ebooklib.importing.domain.model.ImportSessionId
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUpload
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadId
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class StagedEbookUploadRepositoryAdapter(
    private val jpaRepository: StagedEbookUploadJpaRepository,
    private val mapper: StagedEbookUploadEntityMapper,
    private val objectMapper: ObjectMapper
) : StagedEbookUploadRepositoryPort {

    override fun save(stagedEbookUpload: StagedEbookUpload): StagedEbookUpload {
        val entity = mapper.toEntity(stagedEbookUpload)
        val savedEntity = jpaRepository.save(entity)
        return mapper.toDomain(savedEntity)
    }

    override fun findById(id: StagedEbookUploadId): StagedEbookUpload? {
        return jpaRepository.findById(id.value)
            .map { mapper.toDomain(it) }
            .orElse(null)
    }

    override fun delete(id: StagedEbookUploadId) {
        jpaRepository.deleteById(id.value)
    }

    override fun findByExpiryAtBefore(now: Instant): List<StagedEbookUpload> {
        return jpaRepository.findByExpiryAtBefore(now)
            .map { mapper.toDomain(it) }
    }

    override fun findAll(): List<StagedEbookUpload> {
        return jpaRepository.findAll()
            .map { mapper.toDomain(it) }
    }

    override fun count(): Long = jpaRepository.count()

    override fun countByExpiryAtBefore(now: Instant): Long = jpaRepository.countAllByExpiryAtBefore(now)

    override fun findAllKeys(): List<String> {
        val allEntities = jpaRepository.findAll()
        val keys = mutableListOf<String>()
        allEntities.forEach { entity ->
            keys.add("staged/${entity.id}")
            entity.metadataJson?.let { json ->
                try {
                    val map = objectMapper.readValue(json, Map::class.java)
                    val coverKey = map["coverStorageKey"] as? String
                    if (coverKey != null) keys.add(coverKey)
                } catch (e: Exception) {
                    // Ignore malformed metadata
                }
            }
        }
        return keys
    }

    override fun findByImportSessionId(importSessionId: com.mikesajak.ebooklib.importing.domain.model.ImportSessionId): List<StagedEbookUpload> =
        jpaRepository.findAllByImportSessionId(importSessionId.value).map { mapper.toDomain(it) }

    override fun deleteByImportSessionId(importSessionId: com.mikesajak.ebooklib.importing.domain.model.ImportSessionId) {
        jpaRepository.deleteAllByImportSessionId(importSessionId.value)
    }

    override fun findByResolutionItemId(resolutionItemId: UUID): List<StagedEbookUpload> =
        jpaRepository.findAllByResolutionItemId(resolutionItemId).map { mapper.toDomain(it) }
}
