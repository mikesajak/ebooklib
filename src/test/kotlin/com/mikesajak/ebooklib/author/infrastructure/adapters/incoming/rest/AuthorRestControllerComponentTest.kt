package com.mikesajak.ebooklib.author.infrastructure.adapters.incoming.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.mikesajak.ebooklib.author.application.ports.incoming.UpdateAuthorCommand
import com.mikesajak.ebooklib.author.application.services.AuthorService
import com.mikesajak.ebooklib.author.application.ports.incoming.DeleteAuthorUseCase
import com.mikesajak.ebooklib.author.domain.exception.AuthorNotFoundException
import com.mikesajak.ebooklib.author.domain.model.Author
import com.mikesajak.ebooklib.author.domain.model.AuthorId
import com.mikesajak.ebooklib.author.infrastructure.adapters.incoming.rest.dto.AuthorRequestDto
import com.mikesajak.ebooklib.book.application.ports.incoming.GetBooksByAuthorUseCase
import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.BookRestMapper
import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.series.application.ports.incoming.GetSeriesUseCase
import com.mikesajak.ebooklib.series.infrastructure.adapters.incoming.rest.SeriesRestMapper
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import org.mockito.kotlin.eq
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.*

@WebMvcTest(AuthorRestController::class)
@Import(AuthorRestMapper::class, BookRestMapper::class, SeriesRestMapper::class)
class AuthorRestControllerComponentTest(@Autowired val mockMvc: MockMvc,
                                        @Autowired val objectMapper: ObjectMapper
) {

    @MockitoBean
    private lateinit var authorService: AuthorService

    @MockitoBean
    private lateinit var getBooksByAuthorUseCase: GetBooksByAuthorUseCase

    @MockitoBean
    private lateinit var getSeriesUseCase: GetSeriesUseCase

    @Test
    fun `should return author when author exists`() {
        // given
        val authorId = AuthorId(UUID.randomUUID())
        val author = Author(id = authorId, firstName = "Test", lastName = "Author",
                            bio = null, birthDate = null, deathDate = null)

        whenever(authorService.getAuthor(authorId)).thenReturn(author)

        // when
        mockMvc.perform(get("/api/authors/{id}", authorId.value))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(authorId.value.toString()))
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("Author"))
    }

    @Test
    fun `should return 404 when author does not exist`() {
        // given
        val authorId = AuthorId(UUID.randomUUID())

        whenever(authorService.getAuthor(authorId)).thenThrow(AuthorNotFoundException(authorId))

        // when
        mockMvc.perform(get("/api/authors/{id}", authorId.value))
                .andExpect(status().isNotFound)
    }

    @Test
    fun `should return all authors`() {
        // given
        val authors = listOf(
                Author(id = AuthorId(UUID.randomUUID()), firstName = "Test1", lastName = "Author1",
                       bio = null, birthDate = null, deathDate = null),
                Author(id = AuthorId(UUID.randomUUID()), firstName = "Test2", lastName = "Author2",
                       bio = null, birthDate = null, deathDate = null)
        )
        val paginatedResult = PaginatedResult(authors, 0, 10, 2, 1)
        whenever(authorService.getAllAuthors(any())).thenReturn(paginatedResult)

        // when
        mockMvc.perform(get("/api/authors"))
                .andDo { print(it.response.contentAsString) }
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content").isArray)
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(10))
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.page.totalPages").value(1))
    }

    @Test
    fun `should return books by author`() {
        // given
        val authorId = AuthorId(UUID.randomUUID())
        val books = listOf(
                Book(id = BookId(UUID.randomUUID()), title = "Book 1", authors = emptyList(),
                     creationDate = null, publicationDate = null, publisher = null, description = null,
                     series = null, volume = null, labels = emptyList()),
                Book(id = BookId(UUID.randomUUID()), title = "Book 2", authors = emptyList(),
                     creationDate = null, publicationDate = null, publisher = null, description = null,
                     series = null, volume = null, labels = emptyList())
        )
        val paginatedResult = PaginatedResult(books, 0, 10, 2, 1)
        whenever(getBooksByAuthorUseCase.getBooksByAuthor(any(), any())).thenReturn(paginatedResult)

        // when
        mockMvc.perform(get("/api/authors/{id}/books", authorId.value))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content").isArray)
                .andExpect(jsonPath("$.content.length()").value(2))
    }

    @Test
    fun `should save author`() {
        // given
        val authorRequest =
            AuthorRequestDto(firstName = "New", lastName = "Author", bio = null, birthDate = null, deathDate = null)
        val author =
            Author(id = null, firstName = "New", lastName = "Author", bio = null, birthDate = null, deathDate = null)
        val savedAuthor = author.copy(id = AuthorId(UUID.randomUUID()))
        whenever(authorService.saveAuthor(any())).thenReturn(savedAuthor)

        // when
        mockMvc.perform(post("/api/authors")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(authorRequest)))
                .andExpect(status().isCreated)
                .andExpect(header().string("Location", "/api/authors/${savedAuthor.id!!.value}"))
                .andExpect(jsonPath("$.id").value(savedAuthor.id.value.toString()))
                .andExpect(jsonPath("$.firstName").value("New"))
                .andExpect(jsonPath("$.lastName").value("Author"))
    }

    @Test
    fun `should update author`() {
        // given
        val authorId = AuthorId(UUID.randomUUID())
        val authorRequest = AuthorRequestDto(firstName = "Updated", lastName = "Name", bio = "updated bio",
            birthDate = null, deathDate = null)
        val updatedAuthor = Author(id = authorId, firstName = "Updated", lastName = "Name", bio = "updated bio",
            birthDate = null, deathDate = null)
        whenever(authorService.updateAuthor(any())).thenReturn(updatedAuthor)

        // when
        mockMvc.perform(put("/api/authors/{id}", authorId.value)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(authorRequest)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(authorId.value.toString()))
            .andExpect(jsonPath("$.firstName").value("Updated"))
            .andExpect(jsonPath("$.lastName").value("Name"))
            .andExpect(jsonPath("$.bio").value("updated bio"))
    }

    @Test
    fun `should return 404 when updating non-existent author`() {
        // given
        val authorId = AuthorId(UUID.randomUUID())
        val authorRequest = AuthorRequestDto(firstName = "Updated", lastName = "Name", bio = null,
            birthDate = null, deathDate = null)

        whenever(authorService.updateAuthor(any())).thenThrow(AuthorNotFoundException(authorId))

        // when
        mockMvc.perform(put("/api/authors/{id}", authorId.value)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(authorRequest)))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should delete author when author exists`() {
        // given
        val authorId = AuthorId(UUID.randomUUID())

        doNothing().whenever(authorService).deleteAuthor(eq(authorId))

        // when
        mockMvc.perform(delete("/api/authors/{id}", authorId.value))
            .andExpect(status().isNoContent)

        // then
        verify(authorService).deleteAuthor(eq(authorId))
    }

    @Test
    fun `should return 404 when deleting non-existent author`() {
        // given
        val authorId = AuthorId(UUID.randomUUID())

        doThrow(AuthorNotFoundException(authorId)).whenever(authorService).deleteAuthor(eq(authorId))

        // when
        mockMvc.perform(delete("/api/authors/{id}", authorId.value))
            .andExpect(status().isNotFound)

        // then
        verify(authorService).deleteAuthor(eq(authorId))
    }
}
