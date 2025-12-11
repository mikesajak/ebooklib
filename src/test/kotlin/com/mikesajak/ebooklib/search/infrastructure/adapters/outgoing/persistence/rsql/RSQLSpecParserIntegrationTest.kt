package com.mikesajak.ebooklib.search.infrastructure.adapters.outgoing.persistence.rsql

import com.mikesajak.ebooklib.author.infrastructure.adapters.outgoing.persistence.AuthorEntity
import com.mikesajak.ebooklib.author.infrastructure.adapters.outgoing.persistence.AuthorJpaRepository
import com.mikesajak.ebooklib.book.infrastructure.adapters.outgoing.persistence.BookEntity
import com.mikesajak.ebooklib.book.infrastructure.adapters.outgoing.persistence.BookJpaRepository
import com.mikesajak.ebooklib.config.BaseIntegrationTest
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileStoragePort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.domain.Specification
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import software.amazon.awssdk.services.s3.S3Client
import java.time.LocalDate
import java.util.UUID

@ActiveProfiles("test")
@TestPropertySource(properties = [
    "minio.endpoint=http://localhost:9000",
    "minio.access-key=testaccesskey",
    "minio.secret-key=testsecretkey",
    "minio.bucket-name=test-bucket"
])
class RSQLSpecParserIntegrationTest : BaseIntegrationTest() {



    @Autowired
    private lateinit var bookRepository: BookJpaRepository

    @Autowired
    private lateinit var authorRepository: AuthorJpaRepository

    @Autowired
    private lateinit var rsqlSpecParser: RSQLSpecParser

    @MockitoBean
    private lateinit var fileStoragePort: FileStoragePort

    @MockitoBean
    private lateinit var s3Client: S3Client

    private lateinit var author1: AuthorEntity
    private lateinit var author2: AuthorEntity
    private lateinit var book1: BookEntity
    private lateinit var book2: BookEntity

    @BeforeEach
    fun setUp() {
        bookRepository.deleteAll()
        authorRepository.deleteAll()

        author1 = AuthorEntity(id = UUID.randomUUID(), firstName = "John", lastName = "Doe", null, null, null)
        author2 = AuthorEntity(id = UUID.randomUUID(), firstName = "Jane", lastName = "Smith", null, null, null)
        authorRepository.saveAll(listOf(author1, author2))

        book1 = BookEntity(id = UUID.randomUUID(),
            title = "The Hobbit",
            authors = setOf(author1),
            creationDate = LocalDate.now(),
            publicationDate = LocalDate.of(1937, 9, 21),
            publisher = "Allen & Unwin",
            description = "A fantasy novel",
            series = null,
            volume = null,
            labels = setOf("fantasy", "adventure"))
        book2 = BookEntity(id = UUID.randomUUID(),
            title = "The Lord of the Rings",
            authors = setOf(author1, author2),
            creationDate = LocalDate.now(),
            publicationDate = LocalDate.of(1954, 7, 29),
            publisher = "Allen & Unwin",
            description = "An epic high-fantasy novel",
            series = null,
            volume = null,
            labels = setOf("fantasy", "epic"))
        bookRepository.saveAll(listOf(book1, book2))
    }

    @Test
    fun `should return books with matching title`() {
        // Given
        val query = "title==\"The Hobbit\""
        val spec = rsqlSpecParser.parse<BookEntity>(query)

        // When
        val results = bookRepository.findAll(spec)

        // Then
        assertEquals(1, results.size)
        assertEquals("The Hobbit", results[0].title)
    }

    @Test
    fun `should return books with matching author's last name`() {
        // Given
        val query = "authors.lastName==\"Smith\""
        val spec = rsqlSpecParser.parse<BookEntity>(query)

        // When
        val results = bookRepository.findAll(spec)

        // Then
        assertEquals(1, results.size)
        assertEquals("The Lord of the Rings", results[0].title)
    }

    @Test
    fun `should handle AND operator correctly`() {
        // Given
        val query = "title==\"The Lord of the Rings\";authors.lastName==\"Doe\""
        val spec = rsqlSpecParser.parse<BookEntity>(query)

        // When
        val results = bookRepository.findAll(spec)

        // Then
        assertEquals(1, results.size)
        assertEquals("The Lord of the Rings", results[0].title)
    }

    @Test
    fun `should handle OR operator correctly`() {
        // Given
        val query = "authors.lastName==\"Smith\",authors.lastName==\"Doe\""
        val spec = rsqlSpecParser.parse<BookEntity>(query)

        // When
        val results = bookRepository.findAll(spec)

        // Then
        assertEquals(2, results.size)
    }

    @Test
    fun `should handle LIKE operator`() {
        // Given
        val query = "title=like=\"Hobbit\""
        val spec: Specification<BookEntity> = rsqlSpecParser.parse(query)

        // When
        val results = bookRepository.findAll(spec)

        // Then
        assertEquals(1, results.size)
        assertEquals("The Hobbit", results[0].title)
    }
}
