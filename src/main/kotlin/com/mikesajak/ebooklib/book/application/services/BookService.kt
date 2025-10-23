package com.mikesajak.ebooklib.book.application.services

import com.mikesajak.ebooklib.book.application.ports.incoming.GetBookUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.AddBookUseCase
import com.mikesajak.ebooklib.book.application.ports.outgoing.BookRepositoryPort
import com.mikesajak.ebooklib.book.domain.exception.BookNotFoundException
import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.domain.model.BookId
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class BookService(private val bookRepository: BookRepositoryPort) : GetBookUseCase, AddBookUseCase {
    private val logger = KotlinLogging.logger {}

    override fun getBook(bookId: BookId): Book {
        val book = bookRepository.findById(bookId)
            ?: throw BookNotFoundException("Book with id $bookId not found")
        return book
    }

    override fun getAllBooks(): List<Book> {
        return bookRepository.findAll()
    }

    override fun addBook(book: Book): Book {
        return bookRepository.save(book)
    }

}
