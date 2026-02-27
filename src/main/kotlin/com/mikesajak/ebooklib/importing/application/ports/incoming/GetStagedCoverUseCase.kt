package com.mikesajak.ebooklib.importing.application.ports.incoming

import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadId
import java.io.InputStream

data class StagedCover(
    val inputStream: InputStream,
    val contentType: String
)

interface GetStagedCoverUseCase {
    fun getCover(uploadId: StagedEbookUploadId): StagedCover?
}
