package com.mikesajak.ebooklib.importing.application.ports.incoming

import com.mikesajak.ebooklib.importing.domain.model.ResolutionItemId
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadId

interface GroupUploadUseCase {
    fun group(uploadId: StagedEbookUploadId, title: String, authors: List<String>): ResolutionItemId
}
