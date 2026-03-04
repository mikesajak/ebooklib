package com.mikesajak.ebooklib.importing.application.ports.incoming

import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUpload
import java.io.InputStream
import java.util.*

interface UploadToStagingUseCase {
    fun upload(fileContent: InputStream, fileName: String, contentType: String, currentBookId: UUID?): StagedEbookUpload

    fun uploadAsync(fileContent: InputStream, fileName: String, contentType: String, currentBookId: UUID?): StagedEbookUpload
    }
