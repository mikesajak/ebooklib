package com.mikesajak.ebooklib.importing

import com.fasterxml.jackson.databind.ObjectMapper
import com.mikesajak.ebooklib.author.application.ports.incoming.SaveAuthorUseCase
import com.mikesajak.ebooklib.author.domain.model.Author
import com.mikesajak.ebooklib.book.application.ports.incoming.AddBookUseCase
import com.mikesajak.ebooklib.book.application.ports.incoming.GetBookUseCase
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.config.BaseIntegrationTest
import com.mikesajak.ebooklib.importing.infrastructure.adapters.incoming.rest.dto.FinalizeImportRequestDto
import com.mikesajak.ebooklib.importing.infrastructure.adapters.incoming.rest.dto.StagedUploadResponseDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@AutoConfigureMockMvc
@ActiveProfiles("test")
class ImportWorkflowIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var saveAuthorUseCase: SaveAuthorUseCase

    @Autowired
    private lateinit var getBookUseCase: GetBookUseCase

    @Autowired
    private lateinit var addBookUseCase: AddBookUseCase

    @Test
    @Transactional
    fun `should perform full import cycle for a new book`() {
        // 1. Create an author in the system
        val author = saveAuthorUseCase.saveAuthor(Author(null, "Test", "Author", null, null, null))
        val authorId = author.id!!.value

        // 2. Prepare a dummy EPUB file
        val epubData = createSimpleEpub("Full Cycle Book", "Test Author")
        val multipartFile = MockMultipartFile("file", "test.epub", "application/epub+zip", epubData)

        // 3. Upload to staging
        val uploadResult = mockMvc.perform(multipart("/api/import/upload").file(multipartFile))
            .andExpect(status().isOk)
            .andReturn()

        val stagedUpload = objectMapper.readValue(uploadResult.response.contentAsString, StagedUploadResponseDto::class.java)
        assertThat(stagedUpload.metadata["title"]).isEqualTo("Full Cycle Book")

        // 4. Finalize the import
        val finalizeRequest = FinalizeImportRequestDto(
            uploadId = UUID.fromString(stagedUpload.id),
            title = "Finalized Title",
            authorIds = listOf(authorId),
            publisher = "Finalized Publisher",
            description = "Finalized Description"
        )

        val finalizeResult = mockMvc.perform(post("/api/import/finalize")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(finalizeRequest)))
            .andExpect(status().isOk)
            .andReturn()

        val bookId = UUID.fromString(objectMapper.readTree(finalizeResult.response.contentAsString).get("id").asText())

        // 5. Verify results
        val book = getBookUseCase.getBook(BookId(bookId))
        assertThat(book.title).isEqualTo("Finalized Title")
        assertThat(book.authors).hasSize(1)
        assertThat(book.authors[0].id).isEqualTo(author.id)
        assertThat(book.publisher).isEqualTo("Finalized Publisher")
    }

    @Test
    @Transactional
    fun `should perform full import cycle for an existing book (smart add format)`() {
        // 1. Create an author and a book in the system
        val author = saveAuthorUseCase.saveAuthor(Author(null, "Existing", "Author", null, null, null))
        val existingBook = addBookUseCase.addBook(com.mikesajak.ebooklib.book.domain.model.Book(
            id = null,
            title = "Original Title",
            authors = listOf(author),
            creationDate = null,
            publicationDate = null,
            publisher = "Original Publisher",
            description = "Original Description",
            series = null,
            volume = null
        ))
        val bookId = existingBook.id!!.value

        // 2. Prepare a dummy EPUB file
        val epubData = createSimpleEpub("New Title from File", "Existing Author")
        val multipartFile = MockMultipartFile("file", "test.epub", "application/epub+zip", epubData)

        // 3. Upload to staging with currentBookId
        val uploadResult = mockMvc.perform(multipart("/api/import/upload")
            .file(multipartFile)
            .param("currentBookId", bookId.toString()))
            .andExpect(status().isOk)
            .andReturn()

        val stagedUpload = objectMapper.readValue(uploadResult.response.contentAsString, StagedUploadResponseDto::class.java)
        assertThat(stagedUpload.metadata["title"]).isEqualTo("New Title from File")

        // 4. Finalize the import (Update title and publisher)
        val finalizeRequest = FinalizeImportRequestDto(
            uploadId = UUID.fromString(stagedUpload.id),
            bookId = bookId,
            title = "Updated Title",
            authorIds = listOf(author.id!!.value),
            publisher = "Updated Publisher",
            description = "Original Description"
        )

        mockMvc.perform(post("/api/import/finalize")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(finalizeRequest)))
            .andExpect(status().isOk)

        // 5. Verify results
        val updatedBook = getBookUseCase.getBook(BookId(bookId))
        assertThat(updatedBook.title).isEqualTo("Updated Title")
        assertThat(updatedBook.publisher).isEqualTo("Updated Publisher")
    }

    private fun createSimpleEpub(title: String, author: String): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("mimetype"))
            zip.write("application/epub+zip".toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("META-INF/container.xml"))
            zip.write("""<?xml version="1.0"?><container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles></container>""".toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("OEBPS/content.opf"))
            zip.write("""<?xml version="1.0" encoding="UTF-8"?><package xmlns="http://www.idpf.org/2007/opf" unique-identifier="pub-id" version="3.0"><metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:title>$title</dc:title><dc:creator>$author</dc:creator><dc:language>en</dc:language><dc:identifier id="pub-id">test-id</dc:identifier></metadata><manifest><item id="item1" href="text.xhtml" media-type="application/xhtml+xml"/></manifest><spine><itemref idref="item1"/></spine></package>""".toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("OEBPS/text.xhtml"))
            zip.write("""<?xml version="1.0" encoding="UTF-8"?><html xmlns="http://www.w3.org/1999/xhtml"><body><h1>Test</h1></body></html>""".toByteArray())
            zip.closeEntry()
        }
        return out.toByteArray()
    }
}
