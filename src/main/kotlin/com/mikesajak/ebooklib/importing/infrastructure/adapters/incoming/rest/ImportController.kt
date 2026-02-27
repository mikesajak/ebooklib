package com.mikesajak.ebooklib.importing.infrastructure.adapters.incoming.rest

import com.mikesajak.ebooklib.importing.application.ports.incoming.UploadToStagingUseCase
import com.mikesajak.ebooklib.importing.infrastructure.adapters.incoming.rest.dto.StagedUploadResponseDto
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/import")
class ImportController(
    private val uploadToStagingUseCase: UploadToStagingUseCase,
    private val importRestMapper: ImportRestMapper
) {

    @PostMapping("/upload")
    fun uploadFile(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("currentBookId", required = false) currentBookId: UUID?
    ): ResponseEntity<StagedUploadResponseDto> {
        logger.info { "Received upload request for file: ${file.originalFilename}, currentBookId: $currentBookId" }

        val stagedUpload = uploadToStagingUseCase.upload(
            fileContent = file.inputStream,
            fileName = file.originalFilename ?: "untitled",
            contentType = file.contentType ?: "application/octet-stream",
            currentBookId = currentBookId
        )

        return ResponseEntity.ok(importRestMapper.toResponse(stagedUpload))
    }
}
