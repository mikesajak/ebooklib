package com.mikesajak.ebooklib.importing.application.ports.incoming

import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUpload
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadId

interface GetStagedUploadUseCase {
    fun getStagedUpload(id: StagedEbookUploadId): StagedEbookUpload?
}
