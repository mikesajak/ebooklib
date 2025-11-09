package com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest

import com.mikesajak.ebooklib.book.application.ports.incoming.DeleteBookCoverUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.GetBookCoverUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.HasBookCoverUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.UploadBookCoverUseCase
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
    private val uploadBookCoverUseCase: UploadBookCoverUseCase,
    private val getBookCoverUseCase: GetBookCoverUseCase,
    private val deleteBookCoverUseCase: DeleteBookCoverUseCase,
    private val hasBookCoverUseCase: HasBookCoverUseCase
) {
    @PostMapping("/{bookId}/cover")
    fun uploadBookCover(
        @PathVariable bookId: UUID,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<FileMetadata> {
        val fileMetadata = uploadBookCoverUseCase.uploadCover(
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
            val (inputStream, fileMetadata) = getBookCoverUseCase.getCover(BookId(bookId))
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
        deleteBookCoverUseCase.deleteCover(BookId(bookId))
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{bookId}/cover/exists")
    fun hasBookCover(@PathVariable bookId: UUID): ResponseEntity<Map<String, Boolean>> {
        val exists = hasBookCoverUseCase.hasCover(BookId(bookId))
        return ResponseEntity.ok(mapOf("exists" to exists))
    }
}
