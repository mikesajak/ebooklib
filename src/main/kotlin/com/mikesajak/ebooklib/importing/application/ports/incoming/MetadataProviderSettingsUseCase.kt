package com.mikesajak.ebooklib.importing.application.ports.incoming

import com.mikesajak.ebooklib.importing.domain.model.MetadataProviderConfig

interface MetadataProviderSettingsUseCase {
    fun getProvidersConfig(): List<MetadataProviderConfig>
    fun updateProviderConfig(providerId: String, enabled: Boolean, settings: Map<String, String>): MetadataProviderConfig
}
