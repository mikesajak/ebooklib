package com.mikesajak.ebooklib.author.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.author.domain.model.Author
import com.mikesajak.ebooklib.author.domain.model.AuthorId
import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.book.infrastructure.adapters.outgoing.persistence.BookEntityMapper
import com.mikesajak.ebooklib.book.infrastructure.adapters.outgoing.persistence.BookJpaRepository
import com.mikesajak.ebooklib.series.infrastructure.adapters.outgoing.persistence.SeriesEntityMapper
import com.mikesajak.ebooklib.config.TestcontainersConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import java.time.LocalDate
import java.util.*

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.test.context.ContextConfiguration

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = [TestcontainersConfig::class])
@Import(AuthorEntityMapper::class, BookEntityMapper::class, SeriesEntityMapper::class, DbAuthorRepository::class)
class DbAuthorRepositoryIntegrationTest {

    @Autowired
    private lateinit var dbAuthorRepository: DbAuthorRepository

    @Autowired
    private lateinit var authorJpaRepository: AuthorJpaRepository

    @Autowired
    private lateinit var bookJpaRepository: BookJpaRepository

    @Autowired
    private lateinit var authorEntityMapper: AuthorEntityMapper

    @Autowired
    private lateinit var bookEntityMapper: BookEntityMapper

    @BeforeEach
    fun setup() {
        bookJpaRepository.deleteAll()
        authorJpaRepository.deleteAll()
    }

    @Test
    fun `should delete author and remove from associated books`() {
        // given
        val author1Id = UUID.randomUUID()
        val author1 = Author(AuthorId(author1Id), "John", "Doe", "Bio 1", LocalDate.of(1980, 1, 1), null)
        val author1Entity = authorEntityMapper.toEntity(author1)
        authorJpaRepository.save(author1Entity)

        val author2Id = UUID.randomUUID()
        val author2 = Author(AuthorId(author2Id), "Jane", "Smith", "Bio 2", LocalDate.of(1985, 5, 5), null)
        val author2Entity = authorEntityMapper.toEntity(author2)
        authorJpaRepository.save(author2Entity)

        val book1Id = UUID.randomUUID()
        val book1 = Book(BookId(book1Id), "Book 1", listOf(author1, author2), null, null, null, null, null, null, emptyList())
        val book1Entity = bookEntityMapper.toEntity(book1)
        bookJpaRepository.save(book1Entity)

        val book2Id = UUID.randomUUID()
        val book2 = Book(BookId(book2Id), "Book 2", listOf(author1), null, null, null, null, null, null, emptyList())
        val book2Entity = bookEntityMapper.toEntity(book2)
        bookJpaRepository.save(book2Entity)

        val book3Id = UUID.randomUUID()
        val book3 = Book(BookId(book3Id), "Book 3", listOf(author2), null, null, null, null, null, null, emptyList())
        val book3Entity = bookEntityMapper.toEntity(book3)
        bookJpaRepository.save(book3Entity)

        // when
        dbAuthorRepository.deleteById(author1.id!!)

        // then
        assertThat(authorJpaRepository.findById(author1Id)).isEmpty
        assertThat(authorJpaRepository.findById(author2Id)).isPresent

        val updatedBook1 = bookJpaRepository.findById(book1Id).orElse(null)
        assertThat(updatedBook1).isNotNull
        assertThat(updatedBook1.authors).hasSize(1)
        assertThat(updatedBook1.authors.first().id).isEqualTo(author2Id)

        val updatedBook2 = bookJpaRepository.findById(book2Id).orElse(null)
        assertThat(updatedBook2).isNotNull
        assertThat(updatedBook2.authors).isEmpty() // Author 1 was the only author

        val updatedBook3 = bookJpaRepository.findById(book3Id).orElse(null)
        assertThat(updatedBook3).isNotNull
        assertThat(updatedBook3.authors).hasSize(1)
        assertThat(updatedBook3.authors.first().id).isEqualTo(author2Id)
    }

    @Test
    fun `should correctly check if author exists`() {
        // given
        val authorId = UUID.randomUUID()
        val author = Author(AuthorId(authorId), "John", "Doe", "Bio", LocalDate.of(1980, 1, 1), null)
        val authorEntity = authorEntityMapper.toEntity(author)
        authorJpaRepository.save(authorEntity)

        // when & then
        assertThat(dbAuthorRepository.existsById(author.id!!)).isTrue
        assertThat(dbAuthorRepository.existsById(AuthorId(UUID.randomUUID()))).isFalse
    }
}
