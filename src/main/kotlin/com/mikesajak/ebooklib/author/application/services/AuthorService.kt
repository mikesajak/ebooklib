package com.mikesajak.ebooklib.author.application.services

import com.mikesajak.ebooklib.author.application.ports.incoming.GetAuthorUseCase
import com.mikesajak.ebooklib.author.application.ports.incoming.SaveAuthorUseCase
import com.mikesajak.ebooklib.author.application.ports.incoming.UpdateAuthorCommand
import com.mikesajak.ebooklib.author.application.ports.incoming.UpdateAuthorUseCase
import com.mikesajak.ebooklib.author.application.ports.outgoing.AuthorRepositoryPort
import com.mikesajak.ebooklib.author.domain.exception.AuthorNotFoundException
import com.mikesajak.ebooklib.author.domain.model.Author
import com.mikesajak.ebooklib.author.domain.model.AuthorId
import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.common.domain.model.PaginationRequest
import org.springframework.stereotype.Service

@Service
class AuthorService(private val authorRepository: AuthorRepositoryPort)
    : GetAuthorUseCase, SaveAuthorUseCase, UpdateAuthorUseCase {
    override fun getAuthor(authorId: AuthorId): Author {
        val author = authorRepository.findById(authorId)
            ?: throw AuthorNotFoundException(authorId)
        return author
    }

    override fun getAllAuthors(pagination: PaginationRequest): PaginatedResult<Author> {
        return authorRepository.findAll(pagination)
    }

    override fun saveAuthor(author: Author): Author {
        return authorRepository.save(author)
    }

    override fun updateAuthor(command: UpdateAuthorCommand): Author {
        val author = authorRepository.findById(command.id)
            ?: throw AuthorNotFoundException(command.id)

        val updatedAuthor = author.copy(
            firstName = command.firstName,
            lastName = command.lastName,
            bio = command.bio,
            birthDate = command.birthDate,
            deathDate = command.deathDate
        )

        return authorRepository.save(updatedAuthor)
    }
}