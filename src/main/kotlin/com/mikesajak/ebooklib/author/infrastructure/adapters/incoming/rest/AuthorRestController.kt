package com.mikesajak.ebooklib.author.infrastructure.adapters.incoming.rest
import com.mikesajak.ebooklib.book.application.services.BookService
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.BookRestMapper
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.dto.BookResponseDto

import com.mikesajak.ebooklib.author.application.ports.incoming.GetAuthorUseCase
import com.mikesajak.ebooklib.author.application.ports.incoming.SaveAuthorUseCase
import com.mikesajak.ebooklib.author.domain.model.AuthorId
import com.mikesajak.ebooklib.author.infrastructure.adapters.incoming.rest.dto.AuthorRequestDto
import com.mikesajak.ebooklib.author.infrastructure.adapters.incoming.rest.dto.AuthorResponseDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID

@RestController
@RequestMapping("/api/authors")
class AuthorRestController(
    private val getAuthorUseCase: GetAuthorUseCase,
    private val saveAuthorUseCase: SaveAuthorUseCase,
    private val authorRestMapper: AuthorRestMapper
    ,
    private val bookService: BookService,
    private val bookRestMapper: BookRestMapper
) {
    @GetMapping
    fun getAllAuthors(): List<AuthorResponseDto> =
        getAuthorUseCase.getAllAuthors()
            .map { author -> authorRestMapper.toResponse(author) }

    @GetMapping("/{id}")
    fun getAuthorById(@PathVariable id: UUID): AuthorResponseDto =
        authorRestMapper.toResponse(getAuthorUseCase.getAuthor(AuthorId(id)))

    @GetMapping("/{id}/books")
    fun getBooksByAuthor(@PathVariable id: UUID): List<BookResponseDto> =
        bookService.getBooksByAuthor(AuthorId(id))
            .map { book -> bookRestMapper.toResponse(book) }

    @PostMapping
    fun saveAuthor(@RequestBody authorRequestDto: AuthorRequestDto): ResponseEntity<AuthorResponseDto> {
        val author = authorRestMapper.toDomain(authorRequestDto)
        val savedAuthor = saveAuthorUseCase.saveAuthor(author)
        val location = URI.create("/api/authors/${savedAuthor.id?.value}")
        return ResponseEntity.created(location).body(authorRestMapper.toResponse(savedAuthor))
    }
}