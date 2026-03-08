package com.mikesajak.ebooklib.importing.application.ports.incoming

import com.mikesajak.ebooklib.importing.domain.model.SupportedEbookFormat

interface GetSupportedFormatsUseCase {
    fun getSupportedFormats(): List<SupportedEbookFormat>
}
