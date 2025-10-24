package com.mikesajak.ebooklib.book.application.ports.incoming

import com.mikesajak.ebooklib.author.domain.model.AuthorId
import com.mikesajak.ebooklib.book.domain.model.Book

interface GetBooksByAuthorUseCase {
    fun getBooksByAuthor(authorId: AuthorId): List<Book>
}