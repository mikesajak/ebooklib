package com.mikesajak.ebooklib.book.infrastructure.adapters.outgoing.persistence
import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.author.domain.model.Author
import com.mikesajak.ebooklib.author.domain.model.AuthorId

import com.mikesajak.ebooklib.book.application.ports.outgoing.BookRepositoryPort
import mu.KotlinLogging
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*


class DummyBookRepository : BookRepositoryPort {
    private val logger = KotlinLogging.logger {}

    override fun findAll(): List<Book> = listOf(createBook())

    override fun findById(id: BookId): Book = createBook()

    override fun save(book: Book): Book {
        val savedBook = book.copy(id = BookId(UUID.randomUUID()))
        logger.info { "Simulating save of book: ${savedBook.title} by ${savedBook.authors.joinToString { it.name }} with id ${savedBook.id?.value}" }
        return savedBook
    }

    private fun createBook() =
        Book(BookId(UUID.randomUUID()),
            "Example title",
            listOf(Author(AuthorId(UUID.randomUUID()), "Example Author", null, null, null)),
            LocalDate.now(),
            LocalDate.now(),
            "O'Reilly",
            "Example description")
}
