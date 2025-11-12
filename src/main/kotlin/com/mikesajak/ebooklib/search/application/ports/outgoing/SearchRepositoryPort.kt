package com.mikesajak.ebooklib.search.application.ports.outgoing

import com.mikesajak.ebooklib.book.domain.model.Book
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface SearchRepositoryPort {
    fun search(query: String, pageable: Pageable): Page<Book>
}