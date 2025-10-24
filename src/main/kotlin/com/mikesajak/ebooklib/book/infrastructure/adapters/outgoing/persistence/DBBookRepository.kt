package com.mikesajak.ebooklib.book.infrastructure.adapters.outgoing.persistence
import com.mikesajak.ebooklib.series.domain.model.SeriesId

import com.mikesajak.ebooklib.author.domain.model.AuthorId
import com.mikesajak.ebooklib.book.application.ports.outgoing.BookRepositoryPort
import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.domain.model.BookId
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Repository
import java.util.*

@Repository
@Primary
class DBBookRepository(private val bookJpaRepository: BookJpaRepository,
                       private val mapper: BookEntityMapper
) : BookRepositoryPort {

    override fun findAll(): List<Book> =
        bookJpaRepository.findAll().map { bookEntity -> mapper.toDomain(bookEntity) }

    override fun findById(id: BookId): Book? =
        bookJpaRepository.findById(id.value).map { bookEntity -> mapper.toDomain(bookEntity) }.orElse(null)

    override fun save(book: Book): Book {
        val entity = mapper.toEntity(book)
        val entityToSave = if (entity.id == null) entity.copy(id = UUID.randomUUID()) else entity
        val savedEntity = bookJpaRepository.save(entityToSave)
        return mapper.toDomain(savedEntity)
    }

    override fun findByAuthorId(authorId: AuthorId): List<Book> =
        bookJpaRepository.findBooksByAuthorId(authorId.value).map { bookEntity -> mapper.toDomain(bookEntity) }

    override fun findBySeriesId(seriesId: SeriesId): List<Book> =
        bookJpaRepository.findBooksBySeriesId(seriesId.value).map { bookEntity -> mapper.toDomain(bookEntity) }
}
