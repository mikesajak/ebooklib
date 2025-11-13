package com.mikesajak.ebooklib.book.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.author.domain.model.AuthorId
import com.mikesajak.ebooklib.book.application.ports.outgoing.BookRepositoryPort
import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.common.domain.model.PaginationRequest
import com.mikesajak.ebooklib.infrastructure.web.toDomainPage
import com.mikesajak.ebooklib.infrastructure.web.toSpringPageable
import com.mikesajak.ebooklib.series.domain.model.SeriesId
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class BookRepositoryAdapter(
        private val bookJpaRepository: BookJpaRepository,
        private val mapper: BookEntityMapper
) : BookRepositoryPort {

    override fun findAll(pagination: PaginationRequest): PaginatedResult<Book> =
        bookJpaRepository.findAll(pagination.toSpringPageable())
                .toDomainPage { bookEntity -> mapper.toDomain(bookEntity) }

    override fun findById(id: BookId): Book? =
        bookJpaRepository.findById(id.value).map { bookEntity -> mapper.toDomain(bookEntity) }.orElse(null)

    override fun save(book: Book): Book {
        val entity = mapper.toEntity(book)
        val entityToSave = if (entity.id == null) entity.copy(id = UUID.randomUUID()) else entity
        val savedEntity = bookJpaRepository.save(entityToSave)
        return mapper.toDomain(savedEntity)
    }

    override fun update(book: Book): Book {
        bookJpaRepository.findById(book.id!!.value)
                .orElseThrow { NoSuchElementException("Book with id ${book.id.value} not found") }
        val entity = mapper.toEntity(book)
        val savedEntity = bookJpaRepository.save(entity)
        return mapper.toDomain(savedEntity)
    }

    override fun findByAuthorId(authorId: AuthorId, pagination: PaginationRequest): PaginatedResult<Book> =
        bookJpaRepository.findBooksByAuthorId(authorId.value, pagination.toSpringPageable())
                .toDomainPage { bookEntity -> mapper.toDomain(bookEntity) }

    override fun findBySeriesId(seriesId: SeriesId, pagination: PaginationRequest): PaginatedResult<Book> =
        bookJpaRepository.findBooksBySeriesId(seriesId.value, pagination.toSpringPageable())
                .toDomainPage { bookEntity -> mapper.toDomain(bookEntity) }

    override fun delete(bookId: BookId) {
        bookJpaRepository.deleteById(bookId.value)
    }
}
