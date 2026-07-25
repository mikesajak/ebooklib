package com.mikesajak.ebooklib.importing.application.ports.incoming

import com.mikesajak.ebooklib.importing.domain.model.ImportSessionId
import com.mikesajak.ebooklib.importing.domain.model.ResolutionItem
import com.mikesajak.ebooklib.importing.domain.model.ResolutionItemId
import com.mikesajak.ebooklib.importing.domain.model.ResolutionItemStatus

import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadId

interface ResolutionItemUseCase {
    fun getResolutionItems(sessionId: ImportSessionId): List<ResolutionItem>
    fun getResolutionItem(id: ResolutionItemId): ResolutionItem?
    fun updateStatus(id: ResolutionItemId, status: ResolutionItemStatus): ResolutionItem
    fun updateMetadata(id: ResolutionItemId, metadataJson: String?): ResolutionItem
    fun bulkUpdateStatus(ids: List<ResolutionItemId>, status: ResolutionItemStatus)
    fun detachFormat(uploadId: StagedEbookUploadId): ResolutionItem
    fun mergeItems(primaryItemId: ResolutionItemId, sourceItemIds: List<ResolutionItemId>): ResolutionItem
}
