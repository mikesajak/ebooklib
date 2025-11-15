package com.mikesajak.ebooklib.book.application.services

import com.mikesajak.ebooklib.author.application.ports.incoming.GetAuthorUseCase
import com.mikesajak.ebooklib.author.domain.model.AuthorId
import com.mikesajak.ebooklib.book.application.ports.outgoing.BookRepositoryPort
import com.mikesajak.ebooklib.book.domain.exception.BookNotFoundException
import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.common.domain.model.PaginationRequest
import com.mikesajak.ebooklib.series.application.ports.incoming.GetSeriesUseCase
import com.mikesajak.ebooklib.series.domain.model.SeriesId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

class BookServiceTest {
    private val bookRepository = mockk<BookRepositoryPort>()
    private val getAuthorUseCase = mockk<GetAuthorUseCase>()
    private val getSeriesUseCase = mockk<GetSeriesUseCase>()

    private val bookService = BookService(bookRepository, getAuthorUseCase, getSeriesUseCase)

    @Test
    fun `should add a book`() {
        // given
        val book = Book(null, "title", emptyList(), null, null, null, null, null, null, emptyList())
        val savedBook = book.copy(id = BookId(UUID.randomUUID()))
        every { bookRepository.save(book) } returns savedBook

        // when
        val result = bookService.addBook(book)

        // then
        assertThat(result).isEqualTo(savedBook)
        verify { bookRepository.save(book) }
    }

    @Test
    fun `should get a book by id`() {
        // given
        val bookId = BookId(UUID.randomUUID())
        val book = Book(bookId, "title", emptyList(), null, null, null, null, null, null, emptyList())
        every { bookRepository.findById(bookId) } returns book

        // when
        val result = bookService.getBook(bookId)

        // then
        assertThat(result).isEqualTo(book)
        verify { bookRepository.findById(bookId) }
    }

    @Test
    fun `should throw BookNotFoundException when book not found`() {
        // given
        val bookId = BookId(UUID.randomUUID())
        every { bookRepository.findById(bookId) } returns null

        // when, then
        assertThrows<BookNotFoundException> {
            bookService.getBook(bookId)
        }
        verify { bookRepository.findById(bookId) }
    }

    @Test
    fun `should get all books`() {
        // given
        val pagination = PaginationRequest(0, 10)
        val books = listOf(
                Book(BookId(UUID.randomUUID()), "title1", emptyList(),
                     null, null, null, null,
                     null, null, emptyList()),
                Book(BookId(UUID.randomUUID()), "title2", emptyList(),
                     null, null, null, null,
                     null, null, emptyList())
        )
        val paginatedResult = PaginatedResult(books, 0, 10, 2, 1)
        every { bookRepository.findAll(pagination) } returns paginatedResult

        // when
        val result = bookService.getAllBooks(pagination)

        // then
        assertThat(result).isEqualTo(paginatedResult)
        verify { bookRepository.findAll(pagination) }
    }

    @Test
    fun `should get books by author`() {
        // given
        val authorId = AuthorId(UUID.randomUUID())
        val pagination = PaginationRequest(0, 10)
        val books = listOf(
                Book(BookId(UUID.randomUUID()), "title1", emptyList(),
                     null, null, null, null,
                     null, null, emptyList()),
                Book(BookId(UUID.randomUUID()), "title2", emptyList(),
                     null, null, null, null,
                     null, null, emptyList())
        )
        val paginatedResult = PaginatedResult(books, 0, 10, 2, 1)
        every { getAuthorUseCase.getAuthor(authorId) } returns mockk()
        every { bookRepository.findByAuthorId(authorId, pagination) } returns paginatedResult

        // when
        val result = bookService.getBooksByAuthor(authorId, pagination)

        // then
        assertThat(result).isEqualTo(paginatedResult)
        verify { bookRepository.findByAuthorId(authorId, pagination) }
    }

    @Test
    fun `should get books by series`() {
        // given
        val seriesId = SeriesId(UUID.randomUUID())
        val pagination = PaginationRequest(0, 10)
        val books = listOf(
                Book(BookId(UUID.randomUUID()), "title1", emptyList(),
                     null, null, null, null,
                     null, null, emptyList()),
                Book(BookId(UUID.randomUUID()), "title2", emptyList(),
                     null, null, null, null,
                     null, null, emptyList())
        )
        val paginatedResult = PaginatedResult(books, 0, 10, 2, 1)
        every { getSeriesUseCase.getSeries(seriesId) } returns mockk()
        every { bookRepository.findBySeriesId(seriesId, pagination) } returns paginatedResult

        // when
        val result = bookService.getBooksOfSeries(seriesId, pagination)

        // then
        assertThat(result).isEqualTo(paginatedResult)
        verify { bookRepository.findBySeriesId(seriesId, pagination) }
    }

    @Test
    fun `should update a book`() {
        // given
        val bookId = BookId(UUID.randomUUID())
        val book = Book(bookId, "title", emptyList(),
                        null, null, null, null,
                        null, null, emptyList())
        every { bookRepository.save(book) } returns book

        // when
        val result = bookService.updateBook(book)

        // then
        assertThat(result).isEqualTo(book)
        verify { bookRepository.save(book) }
    }

    @Test
    fun `should delete a book`() {
        // given
        val bookId = BookId(UUID.randomUUID())
        every { bookRepository.delete(bookId) } returns Unit

        // when
        bookService.deleteBook(bookId)

        // then
        verify { bookRepository.delete(bookId) }
    }
}
