package com.mikesajak.ebooklib.author.application.ports.incoming

import com.mikesajak.ebooklib.author.domain.model.Author
import com.mikesajak.ebooklib.author.domain.model.AuthorId

interface GetAuthorUseCase {
    fun getAuthor(authorId: AuthorId): Author
    fun getAllAuthors(): List<Author>
}