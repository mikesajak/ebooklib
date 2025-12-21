package com.mikesajak.ebooklib.search.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.author.infrastructure.adapters.outgoing.persistence.AuthorEntity
import com.mikesajak.ebooklib.author.infrastructure.adapters.outgoing.persistence.AuthorJpaRepository
import com.mikesajak.ebooklib.common.domain.model.PaginationRequest
import com.mikesajak.ebooklib.config.BaseIntegrationTest
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileStoragePort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import software.amazon.awssdk.services.s3.S3Client
import java.util.UUID

@ActiveProfiles("test")
@TestPropertySource(properties = [
    "minio.endpoint=http://localhost:9000",
    "minio.access-key=testaccesskey",
    "minio.secret-key=testsecretkey",
    "minio.bucket-name=test-bucket"
])
class SearchAuthorsRepositoryAdapterIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var searchAuthorsRepositoryAdapter: SearchAuthorsRepositoryAdapter

    @Autowired
    private lateinit var authorRepository: AuthorJpaRepository

    @MockitoBean
    private lateinit var fileStoragePort: FileStoragePort

    @MockitoBean
    private lateinit var s3Client: S3Client

    private lateinit var author1: AuthorEntity
    private lateinit var author2: AuthorEntity

    @BeforeEach
    fun setUp() {
        authorRepository.deleteAll()

        author1 = AuthorEntity(id = UUID.randomUUID(), firstName = "John", lastName = "Doe", bio = "Writer", birthDate = null, deathDate = null)
        author2 = AuthorEntity(id = UUID.randomUUID(), firstName = "Jane", lastName = "Smith", bio = "Novelist", birthDate = null, deathDate = null)
        authorRepository.saveAll(listOf(author1, author2))
    }

    @Test
    fun `should search authors by last name`() {
        // Given
        val query = "lastName==\"Doe\""
        val pagination = PaginationRequest(0, 10)

        // When
        val result = searchAuthorsRepositoryAdapter.search(query, pagination)

        // Then
        assertEquals(1, result.totalElements)
        assertEquals("Doe", result.content[0].lastName)
    }

    @Test
    fun `should search authors by first name`() {
        // Given
        val query = "firstName==\"Jane\""
        val pagination = PaginationRequest(0, 10)

        // When
        val result = searchAuthorsRepositoryAdapter.search(query, pagination)

        // Then
        assertEquals(1, result.totalElements)
        assertEquals("Smith", result.content[0].lastName)
    }

    @Test
    fun `should search authors by full name composite`() {
        // Given
        val query = "name=like=\"John Doe\""
        val pagination = PaginationRequest(0, 10)

        // When
        val result = searchAuthorsRepositoryAdapter.search(query, pagination)

        // Then
        assertEquals(1, result.totalElements)
        assertEquals("Doe", result.content[0].lastName)
    }

    @Test
    fun `should search authors by bio`() {
        // Given
        val query = "bio==\"Novelist\""
        val pagination = PaginationRequest(0, 10)

        // When
        val result = searchAuthorsRepositoryAdapter.search(query, pagination)

        // Then
        assertEquals(1, result.totalElements)
        assertEquals("Smith", result.content[0].lastName)
    }

    @Test
    fun `should search authors by description alias`() {
        // Given
        val query = "description==\"Novelist\""
        val pagination = PaginationRequest(0, 10)

        // When
        val result = searchAuthorsRepositoryAdapter.search(query, pagination)

        // Then
        assertEquals(1, result.totalElements)
        assertEquals("Smith", result.content[0].lastName)
    }
}
