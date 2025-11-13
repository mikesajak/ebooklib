package com.mikesajak.ebooklib.book.application.services

import com.mikesajak.ebooklib.author.application.ports.incoming.GetAuthorUseCase
import com.mikesajak.ebooklib.author.domain.model.AuthorId
import com.mikesajak.ebooklib.book.application.ports.incoming.*
import com.mikesajak.ebooklib.book.application.ports.outgoing.BookRepositoryPort
import com.mikesajak.ebooklib.book.domain.exception.BookNotFoundException
import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.common.domain.model.PaginationRequest
import com.mikesajak.ebooklib.series.application.ports.incoming.GetSeriesUseCase
import com.mikesajak.ebooklib.series.domain.model.SeriesId
import org.springframework.stereotype.Service

@Service
class BookService(private val bookRepository: BookRepositoryPort,
                  private val getAuthorsUseCase: GetAuthorUseCase,
                  private val getSeriesUseCase: GetSeriesUseCase
) :
        GetBookUseCase,
        AddBookUseCase,
        UpdateBookUseCase,
        DeleteBookUseCase,
        GetBooksByAuthorUseCase,
        GetBooksBySeriesUseCase {
    override fun getBook(bookId: BookId): Book {
        val book = bookRepository.findById(bookId)
            ?: throw BookNotFoundException(bookId)
        return book
    }

    override fun getAllBooks(pagination: PaginationRequest): PaginatedResult<Book> {
        return bookRepository.findAll(pagination)
    }

    override fun addBook(book: Book): Book {
        return bookRepository.save(book)
    }

    override fun getBooksByAuthor(authorId: AuthorId, pagination: PaginationRequest): PaginatedResult<Book> {
        getAuthorsUseCase.getAuthor(authorId)
        return bookRepository.findByAuthorId(authorId, pagination)
    }

    override fun getBooksOfSeries(seriesId: SeriesId, pagination: PaginationRequest): PaginatedResult<Book> {
        getSeriesUseCase.getSeries(seriesId)
        return bookRepository.findBySeriesId(seriesId, pagination)
    }

    override fun updateBook(book: Book): Book {
        return bookRepository.save(book)
    }

    override fun deleteBook(bookId: BookId) {
        bookRepository.delete(bookId)
    }
}

