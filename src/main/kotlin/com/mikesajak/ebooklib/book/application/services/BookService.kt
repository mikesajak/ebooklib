package com.mikesajak.ebooklib.book.application.services

import com.mikesajak.ebooklib.author.application.ports.incoming.GetAuthorUseCase
import com.mikesajak.ebooklib.author.domain.model.AuthorId
import com.mikesajak.ebooklib.book.application.ports.incoming.*
import com.mikesajak.ebooklib.book.application.ports.outgoing.BookRepositoryPort
import com.mikesajak.ebooklib.book.domain.exception.BookNotFoundException
import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.series.application.ports.incoming.GetSeriesUseCase
import com.mikesajak.ebooklib.series.domain.model.SeriesId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class BookService(private val bookRepository: BookRepositoryPort,
                  private val getAuthorsUseCase: GetAuthorUseCase,
                  private val getSeriesUseCase: GetSeriesUseCase) :
        GetBookUseCase,
        AddBookUseCase,
        UpdateBookUseCase,
        GetBooksByAuthorUseCase,
        GetBooksBySeriesUseCase {
    override fun getBook(bookId: BookId): Book {
        val book = bookRepository.findById(bookId)
            ?: throw BookNotFoundException(bookId)
        return book
    }

    override fun getAllBooks(pageable: Pageable): Page<Book> {
        return bookRepository.findAll(pageable)
    }

    override fun addBook(book: Book): Book {
        return bookRepository.save(book)
    }

    override fun getBooksByAuthor(authorId: AuthorId, pageable: Pageable): Page<Book> {
        getAuthorsUseCase.getAuthor(authorId)
        return bookRepository.findByAuthorId(authorId, pageable)
    }

    override fun getBooksOfSeries(seriesId: SeriesId, pageable: Pageable): Page<Book> {
        getSeriesUseCase.getSeries(seriesId)
        return bookRepository.findBySeriesId(seriesId, pageable)
    }

    override fun updateBook(book: Book): Book {
        return bookRepository.save(book)
    }
}

