package com.mikesajak.ebooklib.importing.application.services

import com.mikesajak.ebooklib.importing.application.ports.incoming.GetStagedUploadUseCase
import com.mikesajak.ebooklib.importing.application.ports.outgoing.StagedEbookUploadRepositoryPort
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUpload
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadId
import org.springframework.stereotype.Service

@Service
class GetStagedUploadService(
    private val repository: StagedEbookUploadRepositoryPort
) : GetStagedUploadUseCase {
    override fun getStagedUpload(id: StagedEbookUploadId): StagedEbookUpload? {
        return repository.findById(id)
    }
}
