package com.mikesajak.ebooklib.importing.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.importing.application.ports.outgoing.StagedEbookUploadRepositoryPort
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUpload
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadId
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class StagedEbookUploadRepositoryAdapter(
    private val jpaRepository: StagedEbookUploadJpaRepository,
    private val mapper: StagedEbookUploadEntityMapper
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

    override fun countByExpiryAtBefore(now: Instant): Long = jpaRepository.countByExpiryAtBefore(now)
}
