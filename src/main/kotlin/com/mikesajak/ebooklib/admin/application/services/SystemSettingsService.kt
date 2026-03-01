package com.mikesajak.ebooklib.admin.application.services

import com.mikesajak.ebooklib.admin.application.ports.incoming.SystemSettingsUseCase
import com.mikesajak.ebooklib.admin.domain.model.SystemSetting
import com.mikesajak.ebooklib.admin.infrastructure.adapters.outgoing.persistence.SystemSettingsEntity
import com.mikesajak.ebooklib.admin.infrastructure.adapters.outgoing.persistence.SystemSettingsJpaRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
@Transactional
class SystemSettingsService(
    private val repository: SystemSettingsJpaRepository
) : SystemSettingsUseCase {

    override fun getAllSettings(): List<SystemSetting> {
        return repository.findAll().map { it.toDomain() }
    }

    override fun getSetting(key: String): SystemSetting? {
        return repository.findById(key).map { it.toDomain() }.orElse(null)
    }

    override fun updateSetting(key: String, value: String?): SystemSetting {
        val entity = repository.findById(key).orElseGet {
            SystemSettingsEntity(key = key)
        }
        entity.value = value
        val saved = repository.save(entity)
        return saved.toDomain()
    }

    private fun SystemSettingsEntity.toDomain() = SystemSetting(
        key = key,
        value = value,
        description = description
    )
}
