package com.mikesajak.ebooklib.author.application.services

import com.mikesajak.ebooklib.author.application.ports.outgoing.AuthorRepositoryPort
import com.mikesajak.ebooklib.author.domain.exception.AuthorNotFoundException
import com.mikesajak.ebooklib.author.domain.model.Author
import com.mikesajak.ebooklib.author.domain.model.AuthorId
import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.common.domain.model.PaginationRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

class AuthorServiceTest {
    private val authorRepositoryPort = mockk<AuthorRepositoryPort>()
    private val authorService = AuthorService(authorRepositoryPort)

    @Test
    fun `should save a new author`() {
        // given
        val author =
            Author(id = null, firstName = "Test", lastName = "Author", bio = null, birthDate = null, deathDate = null)
        val authorId = AuthorId(UUID.randomUUID())
        val savedAuthor = author.copy(id = authorId)

        every { authorRepositoryPort.save(author) } returns savedAuthor

        // when
        val result = authorService.saveAuthor(author)

        // then
        assertThat(result).isEqualTo(savedAuthor)
        verify { authorRepositoryPort.save(author) }
    }

    @Test
    fun `should get an author by id`() {
        // given
        val authorId = AuthorId(UUID.randomUUID())
        val author = Author(id = authorId, firstName = "Test", lastName = "Author",
                            bio = null, birthDate = null, deathDate = null)

        every { authorRepositoryPort.findById(authorId) } returns author

        // when
        val result = authorService.getAuthor(authorId)

        // then
        assertThat(result).isEqualTo(author)
        verify { authorRepositoryPort.findById(authorId) }
    }

    @Test
    fun `should throw AuthorNotFoundException when author not found`() {
        // given
        val authorId = AuthorId(UUID.randomUUID())

        every { authorRepositoryPort.findById(authorId) } returns null

        // when, then
        assertThrows<AuthorNotFoundException> {
            authorService.getAuthor(authorId)
        }
        verify { authorRepositoryPort.findById(authorId) }
    }

    @Test
    fun `should get all authors`() {
        // given
        val pagination = PaginationRequest(0, 10)
        val authors = listOf(
                Author(id = AuthorId(UUID.randomUUID()), firstName = "Test1", lastName = "Author1",
                       bio = null, birthDate = null, deathDate = null),
                Author(id = AuthorId(UUID.randomUUID()), firstName = "Test2", lastName = "Author2",
                       bio = null, birthDate = null, deathDate = null)
        )
        val paginatedResult = PaginatedResult(authors, 0, 10, 2, 1)

        every { authorRepositoryPort.findAll(pagination) } returns paginatedResult

        // when
        val result = authorService.getAllAuthors(pagination)

        // then
        assertThat(result).isEqualTo(paginatedResult)
        verify { authorRepositoryPort.findAll(pagination) }
    }
}
