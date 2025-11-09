package com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest

import com.mikesajak.ebooklib.book.application.ports.incoming.AddEbookFormatUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.DeleteEbookFormatUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.DownloadEbookFormatUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.ListEbookFormatsUseCase
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.dto.EbookFormatFileDto
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

@RestController
@RequestMapping("/api/books")
class BookFormatController(
    private val addEbookFormatUseCase: AddEbookFormatUseCase,
    private val listEbookFormatsUseCase: ListEbookFormatsUseCase,
    private val downloadEbookFormatUseCase: DownloadEbookFormatUseCase,
    private val deleteEbookFormatUseCase: DeleteEbookFormatUseCase
) {
    @PostMapping("/{bookId}/formats")
    fun addEbookFormat(
        @PathVariable bookId: UUID,
        @RequestParam("file") file: MultipartFile,
        @RequestParam("formatType") formatType: String
    ): ResponseEntity<EbookFormatFileDto> {
        val ebookFormatFile = addEbookFormatUseCase.addFormatFile(
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
        val formatFiles = listEbookFormatsUseCase.listFormatFiles(BookId(bookId))
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
        val (inputStream, ebookFormatFile) = downloadEbookFormatUseCase.downloadFormatFile(BookId(bookId), formatFileId)
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
        deleteEbookFormatUseCase.deleteFormatFile(BookId(bookId), formatFileId)
        return ResponseEntity.noContent().build()
    }
}
