package com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

import com.mikesajak.ebooklib.book.application.ports.incoming.AddBookUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.DeleteBookUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.GetBookUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.UpdateBookUseCase
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.dto.BookRequestDto
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.dto.BookResponseDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileMetadata
import com.mikesajak.ebooklib.book.application.services.BookCoverService
import com.mikesajak.ebooklib.book.application.services.EbookFormatService

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
        val (inputStream, fileMetadata) = bookCoverService.getCover(BookId(bookId))
        val resource = InputStreamResource(inputStream)

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(fileMetadata.contentType))
            .contentLength(fileMetadata.size)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${fileMetadata.fileName}\"")
            .body(resource)
    }

    @DeleteMapping("/{bookId}/cover")
    fun deleteBookCover(@PathVariable bookId: UUID): ResponseEntity<Unit> {
        bookCoverService.deleteCover(BookId(bookId))
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{bookId}/formats")
    fun addEbookFormat(
        @PathVariable bookId: UUID,
        @RequestParam("file") file: MultipartFile,
        @RequestParam("formatType") formatType: String
    ): ResponseEntity<FileMetadata> {
        val fileMetadata = ebookFormatService.addFormatFile(
            BookId(bookId),
            file.inputStream,
            file.originalFilename ?: "untitled",
            file.contentType ?: "application/octet-stream",
            formatType
        )
        return ResponseEntity.ok(fileMetadata)
    }

    @GetMapping("/{bookId}/formats")
    fun listEbookFormats(@PathVariable bookId: UUID): ResponseEntity<List<FileMetadata>> {
        val formatFiles = ebookFormatService.listFormatFiles(BookId(bookId))
        return ResponseEntity.ok(formatFiles)
    }

    @GetMapping("/{bookId}/formats/{formatFileId}/download")
    fun downloadEbookFormat(
        @PathVariable bookId: UUID,
        @PathVariable formatFileId: UUID
    ): ResponseEntity<Resource> {
        val (inputStream, fileMetadata) = ebookFormatService.downloadFormatFile(BookId(bookId), formatFileId)
        val resource = InputStreamResource(inputStream)

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
