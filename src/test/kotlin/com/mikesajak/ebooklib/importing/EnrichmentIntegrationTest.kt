package com.mikesajak.ebooklib.importing

import com.fasterxml.jackson.databind.ObjectMapper
import com.mikesajak.ebooklib.config.BaseIntegrationTest
import com.mikesajak.ebooklib.importing.application.ports.incoming.MetadataProviderSettingsUseCase
import com.mikesajak.ebooklib.importing.application.ports.outgoing.StagedEbookUploadRepositoryPort
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadId
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
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.BeforeEach
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestTemplate

@AutoConfigureMockMvc
@ActiveProfiles("test")
class EnrichmentIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var providerSettingsUseCase: MetadataProviderSettingsUseCase

    @Autowired
    private lateinit var stagedUploadRepository: StagedEbookUploadRepositoryPort

    @Autowired
    private lateinit var restTemplate: RestTemplate

    private lateinit var mockServer: MockRestServiceServer

    @BeforeEach
    fun setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate)
    }

    @Test
    @Transactional
    fun `should enrich metadata when provider is enabled`() {
        mockServer.expect(requestTo(startsWith("https://openlibrary.org/search.json")))
            .andRespond(withSuccess("""{"docs":[{"title":"Enrichment Test Book","author_name":["Test Author"],"isbn":["1234567890"],"first_publish_year":"2020"}]}""", MediaType.APPLICATION_JSON))

        // 1. Enable OpenLibrary provider
        providerSettingsUseCase.updateProviderConfig("open_library", true, emptyMap())

        // 2. Prepare and upload a dummy EPUB file
        val epubData = createSimpleEpub("Enrichment Test Book", "Test Author")
        val multipartFile = MockMultipartFile("file", "enrich.epub", "application/epub+zip", epubData)

        val result = mockMvc.perform(multipart("/api/import/upload")
            .file(multipartFile))
            .andExpect(status().isOk)
            .andReturn()

        val response = objectMapper.readValue(result.response.contentAsString, StagedUploadResponseDto::class.java)
        
        // 3. Verify enrichment data in the database
        val upload = stagedUploadRepository.findById(StagedEbookUploadId(UUID.fromString(response.id)))!!
        assertThat(upload.metadataJson).isNotNull()
        
        @Suppress("UNCHECKED_CAST")
        val metadataMap = objectMapper.readValue(upload.metadataJson, Map::class.java) as Map<String, Any?>
        assertThat(metadataMap).containsKey("enrichment")
        
        @Suppress("UNCHECKED_CAST")
        val enrichmentList = metadataMap["enrichment"] as List<Map<String, Any?>>
        assertThat(enrichmentList).isNotEmpty
        
        val firstEnrichment = enrichmentList[0]
        val providerId = firstEnrichment["providerId"] as String
        assertThat(providerId).isEqualTo("open_library")
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
