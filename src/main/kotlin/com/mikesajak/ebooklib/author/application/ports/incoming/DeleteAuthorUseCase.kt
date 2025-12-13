package com.mikesajak.ebooklib.author.application.ports.incoming

import com.mikesajak.ebooklib.author.domain.model.AuthorId

interface DeleteAuthorUseCase {
    fun deleteAuthor(authorId: AuthorId)
}
