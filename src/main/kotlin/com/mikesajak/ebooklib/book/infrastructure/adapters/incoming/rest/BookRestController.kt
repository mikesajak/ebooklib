package com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest

import com.mikesajak.ebooklib.book.application.ports.incoming.AddBookUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.DeleteBookUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.GetBookUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.UpdateBookUseCase
import com.mikesajak.ebooklib.book.application.services.BookCoverService
import com.mikesajak.ebooklib.book.application.services.EbookFormatService
import com.mikesajak.ebooklib.book.domain.exception.BookCoverFileMissingException
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.dto.BookRequestDto
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.dto.BookResponseDto
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileMetadata
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.net.URI
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
class BookRestController(
    private val getBookUseCase: GetBookUseCase,
    private val addBookUseCase: AddBookUseCase,
    private val updateBookUseCase: UpdateBookUseCase,
    private val deleteBookUseCase: DeleteBookUseCase,
    private val bookRestMapper: BookRestMapper,
    private val bookCoverService: BookCoverService,
    private val ebookFormatService: EbookFormatService
) {
    @GetMapping
    fun getAllBooks(pageable: Pageable): Page<BookResponseDto> =
        getBookUseCase.getAllBooks(pageable)
            .map { book -> bookRestMapper.toResponse(book) }

    @GetMapping("/{id}")
    fun getBookById(@PathVariable id: UUID): BookResponseDto {
        val book = getBookUseCase.getBook(BookId(id))
        return bookRestMapper.toResponse(book)
    }

    @PostMapping
    fun saveBook(@RequestBody bookRequestDto: BookRequestDto): ResponseEntity<BookResponseDto> {
        val book = bookRestMapper.toDomain(bookRequestDto)
        val savedBook = addBookUseCase.addBook(book)
        val location = URI.create("/api/books/${savedBook.id!!.value}")
        return ResponseEntity.created(location).body(bookRestMapper.toResponse(savedBook))
    }

    @PutMapping("/{id}")
    fun updateBook(@PathVariable id: UUID, @RequestBody bookRequestDto: BookRequestDto): BookResponseDto {
        val book = bookRestMapper.toDomain(bookRequestDto).copy(id = BookId(id))
        val updatedBook = updateBookUseCase.updateBook(book)
        return bookRestMapper.toResponse(updatedBook)
    }

    @DeleteMapping("/{id}")
    fun deleteBook(@PathVariable id: UUID): ResponseEntity<Unit> {
        deleteBookUseCase.deleteBook(BookId(id))
        return ResponseEntity.noContent().build()
    }

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
            .map { ebookFormatFile ->
                EbookFormatFileDto(
                    id = ebookFormatFile.id.toString(),
                    fileName = ebookFormatFile.fileName,
                    contentType = ebookFormatFile.contentType,
                    size = ebookFormatFile.fileSize,
                    formatType = ebookFormatFile.formatType
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
