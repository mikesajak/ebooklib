package com.mikesajak.ebooklib.importing.application.ports.outgoing

import com.mikesajak.ebooklib.importing.domain.model.EnrichedMetadata

interface MetadataProviderPort {
    fun getProviderId(): String
    fun searchMetadata(title: String, authors: List<String>): List<EnrichedMetadata>
}
