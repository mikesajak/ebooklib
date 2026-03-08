package com.mikesajak.ebooklib.importing.application.services

import com.mikesajak.ebooklib.importing.application.ports.incoming.GetSupportedFormatsUseCase
import com.mikesajak.ebooklib.importing.domain.model.EbookFormats
import com.mikesajak.ebooklib.importing.domain.model.SupportedEbookFormat
import org.springframework.stereotype.Service

@Service
class EbookFormatSupportService : GetSupportedFormatsUseCase {
    override fun getSupportedFormats(): List<SupportedEbookFormat> {
        return EbookFormats.SUPPORTED_FORMATS
    }
}
