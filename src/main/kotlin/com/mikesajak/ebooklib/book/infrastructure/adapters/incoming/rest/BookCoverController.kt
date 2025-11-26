package com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest

import com.mikesajak.ebooklib.book.application.ports.incoming.DeleteBookCoverUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.GetBookCoverUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.UploadBookCoverUseCase
import com.mikesajak.ebooklib.book.domain.exception.BookCoverFileMissingException
import com.mikesajak.ebooklib.book.domain.model.BookCoverMetadata
import com.mikesajak.ebooklib.book.domain.model.BookId
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
        private val deleteBookCoverUseCase: DeleteBookCoverUseCase
) {
    @PostMapping("/{bookId}/cover")
    fun uploadBookCover(
            @PathVariable bookId: UUID,
            @RequestParam("file") file: MultipartFile
    ): ResponseEntity<BookCoverMetadata> {
        val bookCoverMetadata = uploadBookCoverUseCase.uploadCover(
                BookId(bookId),
                file.inputStream,
                file.originalFilename ?: "untitled",
                file.contentType ?: "application/octet-stream"
        )
        return ResponseEntity.ok(bookCoverMetadata)
    }

    @GetMapping("/{bookId}/cover")
    fun getBookCover(@PathVariable bookId: UUID): ResponseEntity<Resource> {
        return try {
            val bookCover = getBookCoverUseCase.getCover(BookId(bookId))
            val resource = InputStreamResource(bookCover.inputStream)

            ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(bookCover.metadata.contentType))
                    .contentLength(bookCover.metadata.fileSize)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${bookCover.metadata.fileName}\"")
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
        val exists = getBookCoverUseCase.hasCover(BookId(bookId))
        return ResponseEntity.ok(mapOf("exists" to exists))
    }
}
