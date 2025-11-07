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

@RestController
@RequestMapping("/api/books")
class BookRestController(
    private val getBookUseCase: GetBookUseCase,
    private val addBookUseCase: AddBookUseCase,
    private val updateBookUseCase: UpdateBookUseCase,
    private val deleteBookUseCase: DeleteBookUseCase,
    private val bookRestMapper: BookRestMapper
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
}
