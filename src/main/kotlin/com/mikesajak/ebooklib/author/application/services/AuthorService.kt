package com.mikesajak.ebooklib.author.application.services
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

import com.mikesajak.ebooklib.author.application.ports.incoming.GetAuthorUseCase
import com.mikesajak.ebooklib.author.application.ports.incoming.SaveAuthorUseCase
import com.mikesajak.ebooklib.author.application.ports.outgoing.AuthorRepositoryPort
import com.mikesajak.ebooklib.author.domain.exception.AuthorNotFoundException
import com.mikesajak.ebooklib.author.domain.model.Author
import com.mikesajak.ebooklib.author.domain.model.AuthorId
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class AuthorService(private val authorRepository: AuthorRepositoryPort) : GetAuthorUseCase, SaveAuthorUseCase {
    override fun getAuthor(authorId: AuthorId): Author {
        val author = authorRepository.findById(authorId)
            ?: throw AuthorNotFoundException(authorId)
        return author
    }

    override fun getAllAuthors(pageable: Pageable): Page<Author> {
        return authorRepository.findAll(pageable)
    }

    override fun saveAuthor(author: Author): Author {
        return authorRepository.save(author)
    }
}