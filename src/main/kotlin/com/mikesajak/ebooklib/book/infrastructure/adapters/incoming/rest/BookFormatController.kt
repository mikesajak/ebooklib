package com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest

import com.mikesajak.ebooklib.book.application.services.EbookFormatService
import com.mikesajak.ebooklib.book.domain.model.BookId
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

data class EbookFormatFileDto(
    val id: String,
    val fileName: String,
    val contentType: String,
    val size: Long,
    val formatType: String
)

@RestController
@RequestMapping("/api/books")
class BookFormatController(
    private val ebookFormatService: EbookFormatService
) {
    @PostMapping("/{bookId}/formats")
    fun addEbookFormat(
        @PathVariable bookId: UUID,
        @RequestParam("file") file: MultipartFile,
        @RequestParam("formatType") formatType: String
    ): ResponseEntity<EbookFormatFileDto> {
        val ebookFormatFile = ebookFormatService.addFormatFile(
            BookId(bookId),
            file.inputStream,
            file.originalFilename ?: "untitled",
            file.contentType ?: "application/octet-stream",
            formatType
        )
        return ResponseEntity.ok(
            EbookFormatFileDto(
                id = ebookFormatFile.id.toString(),
                fileName = ebookFormatFile.fileName,
                contentType = ebookFormatFile.contentType,
                size = ebookFormatFile.fileSize,
                formatType = ebookFormatFile.formatType
            )
        )
    }

    @GetMapping("/{bookId}/formats")
    fun listEbookFormats(@PathVariable bookId: UUID): ResponseEntity<List<EbookFormatFileDto>> {
        val formatFiles = ebookFormatService.listFormatFiles(BookId(bookId))
            .map {
                EbookFormatFileDto(
                    id = it.id.toString(),
                    fileName = it.fileName,
                    contentType = it.contentType,
                    size = it.fileSize,
                    formatType = it.formatType
                )
            }
        return ResponseEntity.ok(formatFiles)
    }

    @GetMapping("/{bookId}/formats/{formatFileId}/download")
    fun downloadEbookFormat(
        @PathVariable bookId: UUID,
        @PathVariable formatFileId: UUID
    ): ResponseEntity<Resource> {
        val (inputStream, ebookFormatFile) = ebookFormatService.downloadFormatFile(BookId(bookId), formatFileId)
        val resource = InputStreamResource(inputStream)
        val fileMetadata = ebookFormatFile.toFileMetadata()

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(fileMetadata.contentType))
            .contentLength(fileMetadata.size)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${fileMetadata.fileName}\"")
            .body(resource)
    }

    @DeleteMapping("/{bookId}/formats/{formatFileId}")
    fun deleteEbookFormat(
        @PathVariable bookId: UUID,
        @PathVariable formatFileId: UUID
    ): ResponseEntity<Unit> {
        ebookFormatService.deleteFormatFile(BookId(bookId), formatFileId)
        return ResponseEntity.noContent().build()
    }
}
