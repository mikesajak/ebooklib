package com.mikesajak.ebooklib.importing.application.ports.incoming

import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUpload
import java.io.InputStream

interface UploadToStagingUseCase {
    fun upload(fileContent: InputStream, fileName: String, contentType: String): StagedEbookUpload
}
