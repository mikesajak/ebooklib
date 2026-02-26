package com.mikesajak.ebooklib.importing.application.ports.incoming

import com.mikesajak.ebooklib.importing.domain.model.ExtractedEbookMetadata
import java.io.InputStream

interface EbookMetadataExtractorUseCase {
    fun extract(fileContent: InputStream, fileName: String, contentType: String): ExtractedEbookMetadata
}
