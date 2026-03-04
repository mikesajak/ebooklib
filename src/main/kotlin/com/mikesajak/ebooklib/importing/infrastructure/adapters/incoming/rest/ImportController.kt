package com.mikesajak.ebooklib.importing.infrastructure.adapters.incoming.rest

import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.BookRestMapper
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.BookView
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.dto.BookResponseDto
import com.mikesajak.ebooklib.importing.application.ports.incoming.FinalizeImportUseCase
import com.mikesajak.ebooklib.importing.application.ports.incoming.GetStagedCoverUseCase
import com.mikesajak.ebooklib.importing.application.ports.incoming.GetStagedUploadUseCase
import com.mikesajak.ebooklib.importing.application.ports.incoming.UploadToStagingUseCase
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadId
import com.mikesajak.ebooklib.importing.infrastructure.adapters.incoming.rest.dto.FinalizeImportRequestDto
import com.mikesajak.ebooklib.importing.infrastructure.adapters.incoming.rest.dto.StagedUploadResponseDto
import mu.KotlinLogging
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
    private val importRestMapper: ImportRestMapper,
    private val bookRestMapper: BookRestMapper
) {

    @PostMapping("/upload")
    fun uploadFile(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("currentBookId", required = false) currentBookId: UUID?,
        @RequestParam("async", required = false, defaultValue = "false") async: Boolean
    ): ResponseEntity<StagedUploadResponseDto> {
        logger.info { "Received upload request for file: ${file.originalFilename}, currentBookId: $currentBookId, async: $async" }

        val stagedUpload = if (async) {
            uploadToStagingUseCase.uploadAsync(
                fileContent = file.inputStream,
                fileName = file.originalFilename ?: "untitled",
                contentType = file.contentType ?: "application/octet-stream",
                currentBookId = currentBookId
            )
        } else {
            uploadToStagingUseCase.upload(
                fileContent = file.inputStream,
                fileName = file.originalFilename ?: "untitled",
                contentType = file.contentType ?: "application/octet-stream",
                currentBookId = currentBookId
            )
        }

        return ResponseEntity.ok(importRestMapper.toResponse(stagedUpload))
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
