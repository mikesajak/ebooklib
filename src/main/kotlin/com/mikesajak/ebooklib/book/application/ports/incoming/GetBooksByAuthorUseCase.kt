package com.mikesajak.ebooklib.book.application.ports.incoming
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

import com.mikesajak.ebooklib.author.domain.model.AuthorId
import com.mikesajak.ebooklib.book.domain.model.Book

interface GetBooksByAuthorUseCase {
    fun getBooksByAuthor(authorId: AuthorId, pageable: Pageable): Page<Book>
}