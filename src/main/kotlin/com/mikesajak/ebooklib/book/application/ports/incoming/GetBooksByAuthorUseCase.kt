package com.mikesajak.ebooklib.book.application.ports.incoming

import com.mikesajak.ebooklib.author.domain.model.AuthorId
import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.common.domain.model.PaginationRequest

interface GetBooksByAuthorUseCase {
    fun getBooksByAuthor(authorId: AuthorId, pagination: PaginationRequest): PaginatedResult<Book>
}