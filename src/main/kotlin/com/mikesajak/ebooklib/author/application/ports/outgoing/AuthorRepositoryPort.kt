package com.mikesajak.ebooklib.author.application.ports.outgoing

import com.mikesajak.ebooklib.author.domain.model.Author
import com.mikesajak.ebooklib.author.domain.model.AuthorId
import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.common.domain.model.PaginationRequest

interface AuthorRepositoryPort {
    fun findAll(pagination: PaginationRequest): PaginatedResult<Author>
    fun findById(id: AuthorId): Author?
    fun save(author: Author): Author
    fun existsById(id: AuthorId): Boolean
    fun deleteById(id: AuthorId)
}