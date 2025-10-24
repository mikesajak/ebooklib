package com.mikesajak.ebooklib.book.application.services

import com.mikesajak.ebooklib.author.application.ports.incoming.GetAuthorUseCase
import com.mikesajak.ebooklib.author.application.ports.outgoing.AuthorRepositoryPort
import com.mikesajak.ebooklib.author.domain.exception.AuthorNotFoundException
import com.mikesajak.ebooklib.author.domain.model.AuthorId

import com.mikesajak.ebooklib.book.application.ports.incoming.GetBookUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.AddBookUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.GetBooksByAuthorUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.GetBooksBySeriesUseCase
import com.mikesajak.ebooklib.book.application.ports.outgoing.BookRepositoryPort
import com.mikesajak.ebooklib.book.domain.exception.BookNotFoundException
import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.series.application.ports.incoming.GetSeriesUseCase
import com.mikesajak.ebooklib.series.domain.model.SeriesId
import org.springframework.stereotype.Service

@Service
class BookService(private val bookRepository: BookRepositoryPort,
                  private val getAuthorsUseCase: GetAuthorUseCase,
                  private val getSeriesUseCase: GetSeriesUseCase) :
        GetBookUseCase,
        AddBookUseCase,
        GetBooksByAuthorUseCase,
        GetBooksBySeriesUseCase {
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

    override fun getBooksByAuthor(authorId: AuthorId): List<Book> {
        getAuthorsUseCase.getAuthor(authorId)
        return bookRepository.findByAuthorId(authorId)
    }

    override fun getBooksOfSeries(seriesId: SeriesId): List<Book> {
        getSeriesUseCase.getSeries(seriesId)
        return bookRepository.findBySeriesId(seriesId)
    }
}

