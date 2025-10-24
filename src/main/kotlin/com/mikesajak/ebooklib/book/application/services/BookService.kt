package com.mikesajak.ebooklib.book.application.services

import com.mikesajak.ebooklib.author.application.ports.outgoing.AuthorRepositoryPort
import com.mikesajak.ebooklib.author.domain.exception.AuthorNotFoundException
import com.mikesajak.ebooklib.author.domain.model.AuthorId
import com.mikesajak.ebooklib.author.infrastructure.adapters.outgoing.persistence.DbAuthorRepository

import com.mikesajak.ebooklib.book.application.ports.incoming.GetBookUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.AddBookUseCase
import com.mikesajak.ebooklib.book.application.ports.outgoing.BookRepositoryPort
import com.mikesajak.ebooklib.book.domain.exception.BookNotFoundException
import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.domain.model.BookId
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class BookService(private val bookRepository: BookRepositoryPort, private val authorRepository: AuthorRepositoryPort) :
        GetBookUseCase, AddBookUseCase {
    override fun getBook(bookId: BookId): Book {
        val book = bookRepository.findById(bookId)
            ?: throw BookNotFoundException(bookId)
        return book
    }

    override fun getAllBooks(): List<Book> {
        return bookRepository.findAll()
    }

    override fun addBook(book: Book): Book {
        return bookRepository.save(book)
    }

    fun getBooksByAuthor(authorId: AuthorId): List<Book> {
        authorRepository.findById(authorId) ?: throw AuthorNotFoundException(authorId)
        return bookRepository.findByAuthorId(authorId)
    }
}

