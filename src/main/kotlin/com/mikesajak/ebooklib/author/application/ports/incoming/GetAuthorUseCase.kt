package com.mikesajak.ebooklib.author.application.ports.incoming
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

import com.mikesajak.ebooklib.author.domain.model.Author
import com.mikesajak.ebooklib.author.domain.model.AuthorId

interface GetAuthorUseCase {
    fun getAuthor(authorId: AuthorId): Author
    fun getAllAuthors(pageable: Pageable): Page<Author>
}