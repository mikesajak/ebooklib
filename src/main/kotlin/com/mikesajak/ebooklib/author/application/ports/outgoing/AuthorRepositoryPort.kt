package com.mikesajak.ebooklib.author.application.ports.outgoing
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

import com.mikesajak.ebooklib.author.domain.model.Author
import com.mikesajak.ebooklib.author.domain.model.AuthorId

interface AuthorRepositoryPort {
    fun findAll(pageable: Pageable): Page<Author>
    fun findById(id: AuthorId): Author?
    fun save(author: Author): Author
}