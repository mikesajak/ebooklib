package com.mikesajak.ebooklib.author.application.ports.outgoing

import com.mikesajak.ebooklib.author.domain.model.Author
import com.mikesajak.ebooklib.author.domain.model.AuthorId

interface AuthorRepositoryPort {
    fun findAll(): List<Author>
    fun findById(id: AuthorId): Author?
    fun save(author: Author): Author
}