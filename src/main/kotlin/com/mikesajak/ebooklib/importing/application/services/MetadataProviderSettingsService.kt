package com.mikesajak.ebooklib.importing.application.services

import com.mikesajak.ebooklib.admin.application.ports.incoming.SystemSettingsUseCase
import com.mikesajak.ebooklib.importing.application.ports.incoming.MetadataProviderSettingsUseCase
import com.mikesajak.ebooklib.importing.domain.model.MetadataProviderConfig
import org.springframework.stereotype.Service

@Service
class MetadataProviderSettingsService(
    private val systemSettingsUseCase: SystemSettingsUseCase
) : MetadataProviderSettingsUseCase {

    private val supportedProviders = listOf(
        ProviderDef("open_library", "Open Library"),
        ProviderDef("google_books", "Google Books")
    )

    override fun getProvidersConfig(): List<MetadataProviderConfig> {
        return supportedProviders.map { def ->
            val enabled = systemSettingsUseCase.getSetting("metadata.provider.${def.id}.enabled")?.value?.toBoolean() ?: false
            // Currently no provider specific settings besides enabled, but could add more here
            MetadataProviderConfig(def.id, def.name, enabled)
        }
    }

    override fun updateProviderConfig(providerId: String, enabled: Boolean, settings: Map<String, String>): MetadataProviderConfig {
        val def = supportedProviders.find { it.id == providerId } ?: throw IllegalArgumentException("Provider $providerId not supported")
        
        systemSettingsUseCase.updateSetting("metadata.provider.$providerId.enabled", enabled.toString())
        // settings could be stored here as well if needed

        return MetadataProviderConfig(def.id, def.name, enabled, settings)
    }

    private data class ProviderDef(val id: String, val name: String)
}
