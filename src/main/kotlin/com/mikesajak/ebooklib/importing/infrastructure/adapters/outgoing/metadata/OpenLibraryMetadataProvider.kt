package com.mikesajak.ebooklib.importing.infrastructure.adapters.outgoing.metadata

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.mikesajak.ebooklib.importing.application.ports.outgoing.MetadataProviderPort
import com.mikesajak.ebooklib.importing.domain.model.EnrichedMetadata
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val logger = KotlinLogging.logger {}

@Component
class OpenLibraryMetadataProvider(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper
) : MetadataProviderPort {

    override fun getProviderId(): String = "open_library"

    override fun searchMetadata(title: String, authors: List<String>): List<EnrichedMetadata> {
        logger.info { "Searching OpenLibrary for title: $title, authors: $authors" }

        val url = UriComponentsBuilder.fromHttpUrl("https://openlibrary.org/search.json")
            .queryParam("title", title)
            .apply {
                if (authors.isNotEmpty()) {
                    queryParam("author", authors.joinToString(" "))
                }
            }
            .build()
            .toUriString()

        return try {
            val response = restTemplate.getForObject(url, String::class.java)
            if (response != null) {
                parseSearchResponse(response)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            logger.error(e) { "Error searching OpenLibrary for $title" }
            emptyList()
        }
    }

    private fun parseSearchResponse(json: String): List<EnrichedMetadata> {
        val root = objectMapper.readTree(json)
        val docs = root.path("docs")
        if (!docs.isArray) return emptyList()

        return docs.take(5).map { doc ->
            val firstIsbn = doc.path("isbn").firstOrNull()?.asText()
            
            EnrichedMetadata(
                providerId = getProviderId(),
                title = doc.path("title").asText(),
                authors = doc.path("author_name").map { it.asText() },
                isbns = doc.path("isbn").map { it.asText() },
                description = null, // Search API doesn't provide full description
                publisher = doc.path("publisher").firstOrNull()?.asText(),
                publicationDate = parseDate(doc.path("first_publish_year").asText()),
                coverUrl = firstIsbn?.let { "https://covers.openlibrary.org/b/isbn/$it-L.jpg" },
                series = null,
                volume = null
            )
        }
    }

    private fun parseDate(yearStr: String): LocalDate? {
        return try {
            if (yearStr.isNotBlank()) {
                LocalDate.of(yearStr.toInt(), 1, 1)
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
