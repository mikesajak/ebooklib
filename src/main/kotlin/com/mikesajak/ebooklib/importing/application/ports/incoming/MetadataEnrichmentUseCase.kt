package com.mikesajak.ebooklib.importing.application.ports.incoming

import com.mikesajak.ebooklib.importing.domain.model.EnrichedMetadata

interface MetadataEnrichmentUseCase {
    fun enrichMetadata(title: String, authors: List<String>): List<EnrichedMetadata>
}
