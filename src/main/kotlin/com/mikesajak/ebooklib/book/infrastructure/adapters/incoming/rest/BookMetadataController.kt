package com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest

import com.mikesajak.ebooklib.book.application.ports.incoming.AddBookUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.DeleteBookUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.GetBookUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.UpdateBookUseCase
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.dto.BookRequestDto
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.dto.BookResponseDto
import com.mikesajak.ebooklib.infrastructure.incoming.rest.dto.PageResponse
import com.mikesajak.ebooklib.infrastructure.incoming.rest.toPageResponse
import com.mikesajak.ebooklib.infrastructure.web.toDomainPagination
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.*

@RestController
@RequestMapping("/api/books")
class BookMetadataController(
        private val getBookUseCase: GetBookUseCase,
        private val addBookUseCase: AddBookUseCase,
        private val updateBookUseCase: UpdateBookUseCase,
        private val deleteBookUseCase: DeleteBookUseCase,
        private val bookRestMapper: BookRestMapper
) {
    @GetMapping
    fun getAllBooks(pageable: Pageable,
                    @RequestParam(name = "view", required = false, defaultValue = "COMPACT") view: BookView
    ): PageResponse<BookResponseDto> {
        return getBookUseCase.getAllBooks(pageable.toDomainPagination())
                .toPageResponse { book -> bookRestMapper.toResponse(book, view) }
    }

    @GetMapping("/{id}")
    fun getBookById(@PathVariable id: UUID): BookResponseDto {
        val book = getBookUseCase.getBook(BookId(id))
        return bookRestMapper.toResponse(book, BookView.FULL)
    }

    @PostMapping
    fun saveBook(@RequestBody bookRequestDto: BookRequestDto): ResponseEntity<BookResponseDto> {
        val book = bookRestMapper.toDomain(bookRequestDto)
        val savedBook = addBookUseCase.addBook(book)
        val location = URI.create("/api/books/${savedBook.id!!.value}")
        return ResponseEntity.created(location).body(bookRestMapper.toResponse(savedBook, BookView.FULL))
    }

    @PutMapping("/{id}")
    fun updateBook(@PathVariable id: UUID, @RequestBody bookRequestDto: BookRequestDto): BookResponseDto {
        val book = bookRestMapper.toDomain(bookRequestDto).copy(id = BookId(id))
        val updatedBook = updateBookUseCase.updateBook(book)
        return bookRestMapper.toResponse(updatedBook, BookView.FULL)
    }

    @DeleteMapping("/{id}")
    fun deleteBook(@PathVariable id: UUID): ResponseEntity<Unit> {
        deleteBookUseCase.deleteBook(BookId(id))
        return ResponseEntity.noContent().build()
    }
}
