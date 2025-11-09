package com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest

import com.mikesajak.ebooklib.book.application.services.BookCoverService
import com.mikesajak.ebooklib.book.domain.exception.BookCoverFileMissingException
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileMetadata
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

@RestController
@RequestMapping("/api/books")
class BookCoverController(
    private val bookCoverService: BookCoverService
) {
    @PostMapping("/{bookId}/cover")
    fun uploadBookCover(
        @PathVariable bookId: UUID,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<FileMetadata> {
        val fileMetadata = bookCoverService.uploadCover(
            BookId(bookId),
            file.inputStream,
            file.originalFilename ?: "untitled",
            file.contentType ?: "application/octet-stream"
        )
        return ResponseEntity.ok(fileMetadata)
    }

    @GetMapping("/{bookId}/cover")
    fun getBookCover(@PathVariable bookId: UUID): ResponseEntity<Resource> {
        return try {
            val (inputStream, fileMetadata) = bookCoverService.getCover(BookId(bookId))
            val resource = InputStreamResource(inputStream)

            ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileMetadata.contentType))
                .contentLength(fileMetadata.size)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${fileMetadata.fileName}\"")
                .body(resource)
        } catch (e: BookCoverFileMissingException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)
        }
    }

    @DeleteMapping("/{bookId}/cover")
    fun deleteBookCover(@PathVariable bookId: UUID): ResponseEntity<Unit> {
        bookCoverService.deleteCover(BookId(bookId))
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{bookId}/cover/exists")
    fun hasBookCover(@PathVariable bookId: UUID): ResponseEntity<Map<String, Boolean>> {
        val exists = bookCoverService.hasCover(BookId(bookId))
        return ResponseEntity.ok(mapOf("exists" to exists))
    }
}
