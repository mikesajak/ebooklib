package com.mikesajak.ebooklib.search.application.ports.incoming

import com.mikesajak.ebooklib.book.domain.model.Book
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface SearchByRSQLUseCase {
    fun search(query: String, pageable: Pageable): Page<Book>
}