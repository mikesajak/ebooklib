package com.mikesajak.ebooklib.importing.application.services

import com.mikesajak.ebooklib.importing.application.ports.incoming.GroupUploadUseCase
import com.mikesajak.ebooklib.importing.application.ports.outgoing.ResolutionItemRepositoryPort
import com.mikesajak.ebooklib.importing.application.ports.outgoing.StagedEbookUploadRepositoryPort
import com.mikesajak.ebooklib.importing.domain.model.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class GroupingService(
    private val resolutionItemRepository: ResolutionItemRepositoryPort,
    private val stagedUploadRepository: StagedEbookUploadRepositoryPort
) : GroupUploadUseCase {

    @Transactional
    override fun group(uploadId: StagedEbookUploadId, title: String, authors: List<String>): ResolutionItemId {
        val upload = stagedUploadRepository.findById(uploadId) ?: throw IllegalArgumentException("Upload $uploadId not found")
        val sessionId = upload.importSessionId

        val normalizedTitle = normalize(title)
        val normalizedAuthors = authors.map { normalize(it) }.sorted()

        // Search for existing ResolutionItem in the same session (if session exists)
        val existing = if (sessionId != null) {
            resolutionItemRepository.findByImportSessionId(sessionId).find { item ->
                normalize(item.title) == normalizedTitle && 
                item.authors.map { normalize(it) }.sorted() == normalizedAuthors
            }
        } else {
            null
        }

        return if (existing != null) {
            val updatedUpload = upload.copy(resolutionItemId = existing.id.value)
            stagedUploadRepository.save(updatedUpload)
            existing.id
        } else {
            // If no session, we still need a session to create a ResolutionItem because of the FK
            // This suggests that we SHOULD have a session for bulk imports.
            // For now, if sessionId is null, we might be in a legacy or single-upload flow.
            // Requirement REQ-003 is about Bulk Import, which implies a session.
            
            if (sessionId == null) {
                // In a real scenario, we might want to create a session here or throw an error.
                // Given the constraints, I'll throw an error or just return a random ID (but it won't persist well without session)
                throw IllegalStateException("Cannot group upload $uploadId without an ImportSession")
            }

            val newItem = ResolutionItem(
                id = ResolutionItemId(UUID.randomUUID()),
                importSessionId = sessionId,
                title = title,
                authors = authors,
                status = ResolutionItemStatus.UNRESOLVED,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            resolutionItemRepository.save(newItem)
            
            val updatedUpload = upload.copy(resolutionItemId = newItem.id.value)
            stagedUploadRepository.save(updatedUpload)
            
            newItem.id
        }
    }

    private fun normalize(s: String): String = s.lowercase(Locale.getDefault()).trim()
}
