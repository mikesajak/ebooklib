package com.mikesajak.ebooklib.author.infrastructure.adapters.incoming.rest

import com.mikesajak.ebooklib.author.application.ports.incoming.GetAuthorUseCase
import com.mikesajak.ebooklib.author.application.ports.incoming.SaveAuthorUseCase
import com.mikesajak.ebooklib.author.application.ports.incoming.UpdateAuthorCommand
import com.mikesajak.ebooklib.author.application.ports.incoming.UpdateAuthorUseCase
import com.mikesajak.ebooklib.author.domain.model.AuthorId
import com.mikesajak.ebooklib.author.infrastructure.adapters.incoming.rest.dto.AuthorRequestDto
import com.mikesajak.ebooklib.author.infrastructure.adapters.incoming.rest.dto.AuthorResponseDto
import com.mikesajak.ebooklib.book.application.ports.incoming.GetBooksByAuthorUseCase
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.BookRestMapper
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.BookView
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
@RequestMapping("/api/authors")
class AuthorRestController(
    private val getAuthorUseCase: GetAuthorUseCase,
    private val saveAuthorUseCase: SaveAuthorUseCase,
    private val updateAuthorUseCase: UpdateAuthorUseCase,
    private val authorRestMapper: AuthorRestMapper,
    private val getBooksByAuthorUseCase: GetBooksByAuthorUseCase,
    private val bookRestMapper: BookRestMapper
) {
    @GetMapping
    fun getAllAuthors(pageable: Pageable): PageResponse<AuthorResponseDto> =
        getAuthorUseCase.getAllAuthors(pageable.toDomainPagination())
            .toPageResponse { author -> authorRestMapper.toResponse(author) }

    @GetMapping("/{id}")
    fun getAuthorById(@PathVariable id: UUID): AuthorResponseDto =
        authorRestMapper.toResponse(getAuthorUseCase.getAuthor(AuthorId(id)))

    @GetMapping("/{id}/books")
    fun getBooksByAuthor(
        @PathVariable id: UUID, pageable: Pageable,
        @RequestParam(name = "view", required = false, defaultValue = "BY_AUTHOR") view: BookView
    ): PageResponse<BookResponseDto> {
        return getBooksByAuthorUseCase.getBooksByAuthor(AuthorId(id), pageable.toDomainPagination())
            .toPageResponse { book -> bookRestMapper.toResponse(book, view) }
    }

    @PostMapping
    fun saveAuthor(@RequestBody authorRequestDto: AuthorRequestDto): ResponseEntity<AuthorResponseDto> {
        val author = authorRestMapper.toDomain(authorRequestDto)
        val savedAuthor = saveAuthorUseCase.saveAuthor(author)
        val location = URI.create("/api/authors/${savedAuthor.id?.value}")
        return ResponseEntity.created(location).body(authorRestMapper.toResponse(savedAuthor))
    }

    @PutMapping("/{id}")
    fun updateAuthor(@PathVariable id: UUID,
                     @RequestBody authorRequestDto: AuthorRequestDto): ResponseEntity<AuthorResponseDto> {
        val command = UpdateAuthorCommand(
            id = AuthorId(id),
            firstName = authorRequestDto.firstName,
            lastName = authorRequestDto.lastName,
            bio = authorRequestDto.bio,
            birthDate = authorRequestDto.birthDate,
            deathDate = authorRequestDto.deathDate
        )
        val updatedAuthor = updateAuthorUseCase.updateAuthor(command)
        return ResponseEntity.ok(authorRestMapper.toResponse(updatedAuthor))
    }
}