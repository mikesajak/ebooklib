package com.mikesajak.ebooklib.importing.application.services

import com.mikesajak.ebooklib.importing.application.ports.incoming.MetadataEnrichmentUseCase
import com.mikesajak.ebooklib.importing.application.ports.incoming.MetadataProviderSettingsUseCase
import com.mikesajak.ebooklib.importing.application.ports.outgoing.MetadataProviderPort
import com.mikesajak.ebooklib.importing.domain.model.EnrichedMetadata
import org.springframework.stereotype.Service

@Service
class MetadataEnrichmentService(
    private val providerSettingsUseCase: MetadataProviderSettingsUseCase,
    private val providers: List<MetadataProviderPort>
) : MetadataEnrichmentUseCase {

    override fun enrichMetadata(title: String, authors: List<String>): List<EnrichedMetadata> {
        val enabledProviderIds = providerSettingsUseCase.getProvidersConfig()
            .filter { it.enabled }
            .map { it.id }

        return providers
            .filter { enabledProviderIds.contains(it.getProviderId()) }
            .flatMap { it.searchMetadata(title, authors) }
    }
}
