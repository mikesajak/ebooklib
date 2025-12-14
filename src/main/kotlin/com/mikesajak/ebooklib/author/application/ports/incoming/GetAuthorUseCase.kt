package com.mikesajak.ebooklib.author.application.ports.incoming

import com.mikesajak.ebooklib.author.application.projection.AuthorProjection
import com.mikesajak.ebooklib.author.domain.model.Author
import com.mikesajak.ebooklib.author.domain.model.AuthorId
import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.common.domain.model.PaginationRequest

interface GetAuthorUseCase {
    fun getAuthor(authorId: AuthorId): Author
    fun getAllAuthors(pagination: PaginationRequest): PaginatedResult<Author>
    fun getAuthorsWithBookCount(pagination: PaginationRequest): PaginatedResult<AuthorProjection>
}