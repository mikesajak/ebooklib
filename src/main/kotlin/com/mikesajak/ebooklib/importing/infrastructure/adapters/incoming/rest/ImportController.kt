package com.mikesajak.ebooklib.importing.infrastructure.adapters.incoming.rest

import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.BookRestMapper
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.BookView
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.dto.BookResponseDto
import com.mikesajak.ebooklib.importing.application.ports.incoming.*
import com.mikesajak.ebooklib.importing.application.ports.outgoing.StagedEbookUploadRepositoryPort
import com.mikesajak.ebooklib.importing.domain.model.*
import com.mikesajak.ebooklib.importing.infrastructure.adapters.incoming.rest.dto.*
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/import")
class ImportController(
    private val uploadToStagingUseCase: UploadToStagingUseCase,
    private val getStagedCoverUseCase: GetStagedCoverUseCase,
    private val getStagedUploadUseCase: GetStagedUploadUseCase,
    private val finalizeImportUseCase: FinalizeImportUseCase,
    private val autoResolutionUseCase: AutoResolutionUseCase,
    private val importSessionUseCase: ImportSessionUseCase,
    private val resolutionItemUseCase: ResolutionItemUseCase,
    private val getSupportedFormatsUseCase: GetSupportedFormatsUseCase,
    private val stagedUploadRepository: StagedEbookUploadRepositoryPort,
    private val importRestMapper: ImportRestMapper,
    private val bookRestMapper: BookRestMapper
) {

    @GetMapping("/supported-formats")
    fun getSupportedFormats(): List<SupportedEbookFormat> {
        return getSupportedFormatsUseCase.getSupportedFormats()
    }

    @PostMapping("/upload")
    fun uploadFile(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("currentBookId", required = false) currentBookId: UUID?,
        @RequestParam("importSessionId", required = false) importSessionId: UUID?,
        @RequestParam("async", required = false, defaultValue = "false") async: Boolean
    ): ResponseEntity<StagedUploadResponseDto> {
        logger.info { "Received upload request for file: ${file.originalFilename}, currentBookId: $currentBookId, importSessionId: $importSessionId, async: $async" }

        val stagedUpload = if (async) {
            uploadToStagingUseCase.uploadAsync(
                fileContent = file.inputStream,
                fileName = file.originalFilename ?: "untitled",
                contentType = file.contentType ?: "application/octet-stream",
                currentBookId = currentBookId,
                importSessionId = importSessionId
            )
        } else {
            uploadToStagingUseCase.upload(
                fileContent = file.inputStream,
                fileName = file.originalFilename ?: "untitled",
                contentType = file.contentType ?: "application/octet-stream",
                currentBookId = currentBookId,
                importSessionId = importSessionId
            )
        }

        return ResponseEntity.ok(importRestMapper.toResponse(stagedUpload))
    }

    @PostMapping("/staged/{uploadId}/retry")
    fun retryProcessing(@PathVariable uploadId: UUID) {
        uploadToStagingUseCase.retryProcessing(uploadId)
    }

    @PostMapping("/sessions")
    fun createSession(@RequestParam("totalFiles") totalFiles: Int): ImportSessionResponseDto {
        val session = importSessionUseCase.createSession(totalFiles)
        return importRestMapper.toResponse(session)
    }

    @GetMapping("/sessions")
    fun getActiveSessions(): List<ImportSessionResponseDto> {
        return importSessionUseCase.getActiveSessions().map { session ->
            val uploads = stagedUploadRepository.findByImportSessionId(session.id)
            importRestMapper.toResponse(session, uploads)
        }
    }

    @GetMapping("/sessions/{sessionId}")
    fun getSession(@PathVariable sessionId: UUID): ResponseEntity<ImportSessionResponseDto> {
        val session = importSessionUseCase.getSession(ImportSessionId(sessionId))
            ?: return ResponseEntity.notFound().build()
        val uploads = stagedUploadRepository.findByImportSessionId(session.id)
        return ResponseEntity.ok(importRestMapper.toResponse(session, uploads))
    }

    @DeleteMapping("/sessions/{sessionId}")
    fun deleteSession(@PathVariable sessionId: UUID) {
        importSessionUseCase.deleteSession(ImportSessionId(sessionId))
    }

    @PostMapping("/sessions/{sessionId}/finalize")
    fun finalizeSession(@PathVariable sessionId: UUID): ImportSessionResponseDto {
        val session = importSessionUseCase.finalizeSession(ImportSessionId(sessionId))
        return importRestMapper.toResponse(session)
    }

    @PostMapping("/sessions/{sessionId}/auto-resolve")
    fun autoResolve(
        @PathVariable sessionId: UUID,
        @RequestParam("ids", required = false) ids: List<UUID>?,
        @RequestParam("strategy") strategy: AutoResolveStrategy
    ) {
        autoResolutionUseCase.autoResolve(
            ImportSessionId(sessionId),
            ids?.map { ResolutionItemId(it) },
            strategy
        )
    }

    @GetMapping("/sessions/{sessionId}/items")
    fun getResolutionItems(@PathVariable sessionId: UUID): List<ResolutionItemResponseDto> {
        val sessionItems = resolutionItemUseCase.getResolutionItems(ImportSessionId(sessionId))
        val resolutionItemResponses = sessionItems.map { item ->
            val formats = stagedUploadRepository.findByResolutionItemId(item.id.value)
            importRestMapper.toResponse(item, formats)
        }

        val allUploads = stagedUploadRepository.findByImportSessionId(ImportSessionId(sessionId))
        val uploadsWithoutResolutionItem = allUploads.filter { it.resolutionItemId == null }
        
        val syntheticResponses = uploadsWithoutResolutionItem.map { upload ->
            importRestMapper.toResolutionItemResponse(upload)
        }

        return resolutionItemResponses + syntheticResponses
    }

    @GetMapping("/items/{itemId}")
    fun getResolutionItem(@PathVariable itemId: UUID): ResponseEntity<ResolutionItemResponseDto> {
        val item = resolutionItemUseCase.getResolutionItem(ResolutionItemId(itemId))
        if (item != null) {
            val formats = stagedUploadRepository.findByResolutionItemId(item.id.value)
            return ResponseEntity.ok(importRestMapper.toResponse(item, formats))
        }

        val upload = stagedUploadRepository.findById(StagedEbookUploadId(itemId))
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(importRestMapper.toResolutionItemResponse(upload))
    }

    @DeleteMapping("/items/{itemId}")
    fun deleteResolutionItem(@PathVariable itemId: UUID) {
        resolutionItemUseCase.deleteItem(itemId)
    }

    @PatchMapping("/items/{itemId}/status")
    fun updateResolutionItemStatus(
        @PathVariable itemId: UUID,
        @RequestParam("status") status: ResolutionItemStatus
    ): ResolutionItemResponseDto {
        val item = resolutionItemUseCase.updateStatus(ResolutionItemId(itemId), status)
        val formats = stagedUploadRepository.findByResolutionItemId(item.id.value)
        return importRestMapper.toResponse(item, formats)
    }

    @PatchMapping("/items/bulk-status")
    fun bulkUpdateResolutionItemStatus(
        @RequestParam("ids") ids: List<UUID>,
        @RequestParam("status") status: ResolutionItemStatus
    ) {
        resolutionItemUseCase.bulkUpdateStatus(ids.map { ResolutionItemId(it) }, status)
    }

    @PostMapping("/staged/{uploadId}/detach")
    fun detachFormat(@PathVariable uploadId: UUID): ResolutionItemResponseDto {
        val newItem = resolutionItemUseCase.detachFormat(StagedEbookUploadId(uploadId))
        val formats = stagedUploadRepository.findByResolutionItemId(newItem.id.value)
        return importRestMapper.toResponse(newItem, formats)
    }

    @PostMapping("/items/merge")
    fun mergeItems(
        @RequestParam("primaryId") primaryId: UUID,
        @RequestParam("sourceIds") sourceIds: List<UUID>
    ): ResolutionItemResponseDto {
        val mergedItem = resolutionItemUseCase.mergeItems(
            ResolutionItemId(primaryId),
            sourceIds.map { ResolutionItemId(it) }
        )
        val formats = stagedUploadRepository.findByResolutionItemId(mergedItem.id.value)
        return importRestMapper.toResponse(mergedItem, formats)
    }

    @GetMapping("/staged/{uploadId}")
    fun getStagedUpload(@PathVariable uploadId: UUID): ResponseEntity<StagedUploadResponseDto> {
        val upload = getStagedUploadUseCase.getStagedUpload(StagedEbookUploadId(uploadId))
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(importRestMapper.toResponse(upload))
    }

    @GetMapping("/staged/{uploadId}/cover")
    fun getStagedCover(@PathVariable uploadId: UUID): ResponseEntity<InputStreamResource> {
        logger.info { "Requested staged cover for uploadId: $uploadId" }

        val stagedCover = getStagedCoverUseCase.getCover(StagedEbookUploadId(uploadId))
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(stagedCover.contentType))
            .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
            .body(InputStreamResource(stagedCover.inputStream))
    }

    @PostMapping("/finalize")
    fun finalizeImport(@RequestBody request: FinalizeImportRequestDto): BookResponseDto {
        logger.info { "Finalizing import for uploadId: ${request.uploadId}" }

        val command = importRestMapper.toCommand(request)
        val finalizedBook = finalizeImportUseCase.finalize(command)

        return bookRestMapper.toResponse(finalizedBook, BookView.FULL)
    }
}
