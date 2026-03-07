package com.mikesajak.ebooklib.importing.application.services

import com.mikesajak.ebooklib.importing.application.ports.incoming.ResolutionItemUseCase
import com.mikesajak.ebooklib.importing.application.ports.outgoing.ResolutionItemRepositoryPort
import com.mikesajak.ebooklib.importing.domain.model.ImportSessionId
import com.mikesajak.ebooklib.importing.domain.model.ResolutionItem
import com.mikesajak.ebooklib.importing.domain.model.ResolutionItemId
import com.mikesajak.ebooklib.importing.domain.model.ResolutionItemStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class ResolutionItemService(
    private val repository: ResolutionItemRepositoryPort
) : ResolutionItemUseCase {

    override fun getResolutionItems(sessionId: ImportSessionId): List<ResolutionItem> {
        return repository.findByImportSessionId(sessionId)
    }

    override fun getResolutionItem(id: ResolutionItemId): ResolutionItem? {
        return repository.findById(id)
    }

    @Transactional
    override fun updateStatus(id: ResolutionItemId, status: ResolutionItemStatus): ResolutionItem {
        val item = repository.findById(id) ?: throw IllegalArgumentException("ResolutionItem $id not found")
        val updated = item.copy(
            status = status,
            updatedAt = Instant.now()
        )
        return repository.save(updated)
    }

    @Transactional
    override fun updateMetadata(id: ResolutionItemId, metadataJson: String?): ResolutionItem {
        val item = repository.findById(id) ?: throw IllegalArgumentException("ResolutionItem $id not found")
        val updated = item.copy(
            metadataJson = metadataJson,
            updatedAt = Instant.now()
        )
        return repository.save(updated)
    }

    @Transactional
    override fun bulkUpdateStatus(ids: List<ResolutionItemId>, status: ResolutionItemStatus) {
        val now = Instant.now()
        ids.forEach { id ->
            repository.findById(id)?.let { item ->
                repository.save(item.copy(status = status, updatedAt = now))
            }
        }
    }
}
