package com.mikesajak.ebooklib.importing

import com.fasterxml.jackson.databind.ObjectMapper
import com.mikesajak.ebooklib.config.BaseIntegrationTest
import com.mikesajak.ebooklib.importing.application.ports.outgoing.ImportSessionRepositoryPort
import com.mikesajak.ebooklib.importing.application.ports.outgoing.ResolutionItemRepositoryPort
import com.mikesajak.ebooklib.importing.application.ports.outgoing.StagedEbookUploadRepositoryPort
import com.mikesajak.ebooklib.importing.domain.model.ImportSession
import com.mikesajak.ebooklib.importing.domain.model.ImportSessionId
import com.mikesajak.ebooklib.importing.domain.model.ImportSessionStatus
import com.mikesajak.ebooklib.importing.infrastructure.adapters.incoming.rest.dto.StagedUploadResponseDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@AutoConfigureMockMvc
@ActiveProfiles("test")
class GroupingIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var sessionRepository: ImportSessionRepositoryPort

    @Autowired
    private lateinit var resolutionItemRepository: ResolutionItemRepositoryPort

    @Autowired
    private lateinit var stagedUploadRepository: StagedEbookUploadRepositoryPort

    @Test
    @Transactional
    fun `should group multiple uploads into the same resolution item`() {
        // 1. Create an ImportSession
        val sessionId = ImportSessionId(UUID.randomUUID())
        val session = ImportSession(
            id = sessionId,
            status = ImportSessionStatus.ACTIVE,
            totalFiles = 0,
            processedFiles = 0,
            failedFiles = 0,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            expiryAt = Instant.now().plus(1, ChronoUnit.HOURS)
        )
        sessionRepository.save(session)

        // 2. Upload first file
        val epub1 = createSimpleEpub("Common Title", "Common Author")
        val file1 = MockMultipartFile("file", "book1.epub", "application/epub+zip", epub1)
        
        val result1 = mockMvc.perform(multipart("/api/import/upload")
            .file(file1)
            .param("importSessionId", sessionId.value.toString()))
            .andExpect(status().isOk)
            .andReturn()

        val upload1 = objectMapper.readValue(result1.response.contentAsString, StagedUploadResponseDto::class.java)
        
        // 3. Upload second file (same title/author)
        val epub2 = createSimpleEpub("Common Title", "Common Author")
        val file2 = MockMultipartFile("file", "book2.epub", "application/epub+zip", epub2)

        val result2 = mockMvc.perform(multipart("/api/import/upload")
            .file(file2)
            .param("importSessionId", sessionId.value.toString()))
            .andExpect(status().isOk)
            .andReturn()

        val upload2 = objectMapper.readValue(result2.response.contentAsString, StagedUploadResponseDto::class.java)

        // 4. Verify they have the same ResolutionItem ID
        val domainUpload1 = stagedUploadRepository.findById(com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadId(UUID.fromString(upload1.id)))!!
        val domainUpload2 = stagedUploadRepository.findById(com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadId(UUID.fromString(upload2.id)))!!

        assertThat(domainUpload1.resolutionItemId).isNotNull
        assertThat(domainUpload2.resolutionItemId).isNotNull
        assertThat(domainUpload1.resolutionItemId).isEqualTo(domainUpload2.resolutionItemId)

        // 5. Verify ResolutionItem exists
        val resolutionItem = resolutionItemRepository.findById(com.mikesajak.ebooklib.importing.domain.model.ResolutionItemId(domainUpload1.resolutionItemId!!))
        assertThat(resolutionItem).isNotNull
        assertThat(resolutionItem?.title).isEqualTo("Common Title")
        assertThat(resolutionItem?.authors).containsExactly("Common Author")
    }

    @Test
    @Transactional
    fun `should create different resolution items for different books`() {
        // 1. Create an ImportSession
        val sessionId = ImportSessionId(UUID.randomUUID())
        val session = ImportSession(
            id = sessionId,
            status = ImportSessionStatus.ACTIVE,
            totalFiles = 0,
            processedFiles = 0,
            failedFiles = 0,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            expiryAt = Instant.now().plus(1, ChronoUnit.HOURS)
        )
        sessionRepository.save(session)

        // 2. Upload first book
        val epub1 = createSimpleEpub("Book One", "Author A")
        val file1 = MockMultipartFile("file", "book1.epub", "application/epub+zip", epub1)
        
        val result1 = mockMvc.perform(multipart("/api/import/upload")
            .file(file1)
            .param("importSessionId", sessionId.value.toString()))
            .andExpect(status().isOk)
            .andReturn()

        val upload1 = objectMapper.readValue(result1.response.contentAsString, StagedUploadResponseDto::class.java)
        
        // 3. Upload second book (different title)
        val epub2 = createSimpleEpub("Book Two", "Author A")
        val file2 = MockMultipartFile("file", "book2.epub", "application/epub+zip", epub2)

        val result2 = mockMvc.perform(multipart("/api/import/upload")
            .file(file2)
            .param("importSessionId", sessionId.value.toString()))
            .andExpect(status().isOk)
            .andReturn()

        val upload2 = objectMapper.readValue(result2.response.contentAsString, StagedUploadResponseDto::class.java)

        // 4. Verify they have different ResolutionItem IDs
        val domainUpload1 = stagedUploadRepository.findById(com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadId(UUID.fromString(upload1.id)))!!
        val domainUpload2 = stagedUploadRepository.findById(com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadId(UUID.fromString(upload2.id)))!!

        assertThat(domainUpload1.resolutionItemId).isNotNull
        assertThat(domainUpload2.resolutionItemId).isNotNull
        assertThat(domainUpload1.resolutionItemId).isNotEqualTo(domainUpload2.resolutionItemId)
    }

    @Test
    @Transactional
    fun `should group epub and pdf of same book into the same resolution item`() {
        val sessionId = ImportSessionId(UUID.randomUUID())
        val session = ImportSession(
            id = sessionId,
            status = ImportSessionStatus.ACTIVE,
            totalFiles = 0,
            processedFiles = 0,
            failedFiles = 0,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            expiryAt = Instant.now().plus(1, ChronoUnit.HOURS)
        )
        sessionRepository.save(session)

        // 1. Upload EPUB with title & author
        val epubContent = createSimpleEpub("Clean Code", "Robert C. Martin")
        val file1 = MockMultipartFile("file", "Clean Code.epub", "application/epub+zip", epubContent)
        val result1 = mockMvc.perform(multipart("/api/import/upload")
            .file(file1)
            .param("importSessionId", sessionId.value.toString()))
            .andExpect(status().isOk)
            .andReturn()
        val upload1 = objectMapper.readValue(result1.response.contentAsString, StagedUploadResponseDto::class.java)

        // 2. Upload PDF without metadata (mock dummy pdf bytes)
        val file2 = MockMultipartFile("file", "Clean Code.pdf", "application/pdf", "dummy pdf content".toByteArray())
        val result2 = mockMvc.perform(multipart("/api/import/upload")
            .file(file2)
            .param("importSessionId", sessionId.value.toString()))
            .andExpect(status().isOk)
            .andReturn()
        val upload2 = objectMapper.readValue(result2.response.contentAsString, StagedUploadResponseDto::class.java)

        // 3. Verify both staged uploads share the same resolution item
        val domainUpload1 = stagedUploadRepository.findById(com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadId(UUID.fromString(upload1.id)))!!
        val domainUpload2 = stagedUploadRepository.findById(com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadId(UUID.fromString(upload2.id)))!!

        assertThat(domainUpload1.resolutionItemId).isNotNull
        assertThat(domainUpload2.resolutionItemId).isNotNull
        assertThat(domainUpload1.resolutionItemId).isEqualTo(domainUpload2.resolutionItemId)
    }

    @Test
    @Transactional
    fun `should detach a format and merge resolution items`() {
        val sessionId = ImportSessionId(UUID.randomUUID())
        val session = ImportSession(
            id = sessionId,
            status = ImportSessionStatus.ACTIVE,
            totalFiles = 0,
            processedFiles = 0,
            failedFiles = 0,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            expiryAt = Instant.now().plus(1, ChronoUnit.HOURS)
        )
        sessionRepository.save(session)

        val epubContent = createSimpleEpub("Refactoring", "Martin Fowler")
        val file1 = MockMultipartFile("file", "Refactoring.epub", "application/epub+zip", epubContent)
        val result1 = mockMvc.perform(multipart("/api/import/upload")
            .file(file1)
            .param("importSessionId", sessionId.value.toString()))
            .andExpect(status().isOk)
            .andReturn()
        val upload1 = objectMapper.readValue(result1.response.contentAsString, StagedUploadResponseDto::class.java)

        val file2 = MockMultipartFile("file", "Refactoring.pdf", "application/pdf", "dummy pdf".toByteArray())
        val result2 = mockMvc.perform(multipart("/api/import/upload")
            .file(file2)
            .param("importSessionId", sessionId.value.toString()))
            .andExpect(status().isOk)
            .andReturn()
        val upload2 = objectMapper.readValue(result2.response.contentAsString, StagedUploadResponseDto::class.java)

        // Initial state: grouped together
        val domain1 = stagedUploadRepository.findById(com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadId(UUID.fromString(upload1.id)))!!
        val domain2 = stagedUploadRepository.findById(com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadId(UUID.fromString(upload2.id)))!!
        val origResolutionItemId = domain1.resolutionItemId!!
        assertThat(domain1.resolutionItemId).isEqualTo(domain2.resolutionItemId)

        // Detach upload 2
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/import/staged/${upload2.id}/detach"))
            .andExpect(status().isOk)

        val detachedDomain2 = stagedUploadRepository.findById(com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadId(UUID.fromString(upload2.id)))!!
        assertThat(detachedDomain2.resolutionItemId).isNotEqualTo(origResolutionItemId)

        // Now merge them back together
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/import/items/merge")
            .param("primaryId", origResolutionItemId.toString())
            .param("sourceIds", detachedDomain2.resolutionItemId.toString()))
            .andExpect(status().isOk)

        val mergedDomain2 = stagedUploadRepository.findById(com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadId(UUID.fromString(upload2.id)))!!
        assertThat(mergedDomain2.resolutionItemId).isEqualTo(origResolutionItemId)
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
