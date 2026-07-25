package com.mikesajak.ebooklib.importing.infrastructure.adapters.outgoing.extraction

import com.mikesajak.ebooklib.importing.application.ports.incoming.EbookMetadataExtractorUseCase
import com.mikesajak.ebooklib.importing.domain.model.ExtractedCoverImage
import com.mikesajak.ebooklib.importing.domain.model.ExtractedEbookMetadata
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.tika.extractor.EmbeddedDocumentExtractor
import org.apache.tika.metadata.Metadata
import org.apache.tika.metadata.TikaCoreProperties
import org.apache.tika.metadata.XMPDM
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.parser.ParseContext
import org.apache.tika.parser.pdf.PDFParserConfig
import org.apache.tika.sax.BodyContentHandler
import org.springframework.stereotype.Component
import org.xml.sax.ContentHandler
import java.io.InputStream
import java.time.LocalDate
import java.util.*

private val logger = KotlinLogging.logger {}

@Component
class TikaEbookMetadataExtractor : EbookMetadataExtractorUseCase {

    override fun extract(fileContent: InputStream, fileName: String, contentType: String): ExtractedEbookMetadata {
        logger.info { "Extracting metadata from file: $fileName ($contentType)" }

        val parser = AutoDetectParser()
        val handler = BodyContentHandler(-1)
        val metadata = Metadata()
        val context = ParseContext()

        // Disable OCR to avoid NPEs and improve performance when Tesseract is not present
        val pdfConfig = PDFParserConfig()
        pdfConfig.ocrStrategy = PDFParserConfig.OCR_STRATEGY.NO_OCR
        context.set(PDFParserConfig::class.java, pdfConfig)

        val coverImageExtractor = CoverImageExtractor()
        context.set(EmbeddedDocumentExtractor::class.java, coverImageExtractor)

        try {
            parser.parse(fileContent, handler, metadata, context)
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse metadata for file: $fileName" }
        }

        return ExtractedEbookMetadata(
            title = metadata.get(TikaCoreProperties.TITLE),
            authors = metadata.getValues(TikaCoreProperties.CREATOR).toList(),
            creationDate = parseDate(metadata.get(TikaCoreProperties.CREATED)),
            publicationDate = parseDate(metadata.get(XMPDM.RELEASE_DATE)),
            publisher = metadata.get("dc:publisher") ?: metadata.get("publisher"),
            description = metadata.get(TikaCoreProperties.DESCRIPTION),
            coverImage = coverImageExtractor.coverImage
        )
    }

    private fun parseDate(dateStr: String?): LocalDate? {
        if (dateStr == null) return null
        return try {
            val cleanDate = dateStr.substringBefore("T")
            LocalDate.parse(cleanDate)
        } catch (e: Exception) {
            logger.debug { "Failed to parse date string: $dateStr" }
            null
        }
    }

    private class CoverImageExtractor : EmbeddedDocumentExtractor {
        var coverImage: ExtractedCoverImage? = null

        override fun shouldParseEmbedded(metadata: Metadata): Boolean {
            val contentType = metadata.get(Metadata.CONTENT_TYPE)
            val fileName = metadata.get(TikaCoreProperties.RESOURCE_NAME_KEY) ?: ""
            return contentType?.startsWith("image/") ?: false || fileName.contains("cover", ignoreCase = true)
        }

        override fun parseEmbedded(stream: InputStream, handler: ContentHandler, metadata: Metadata, outputHtml: Boolean) {
            if (coverImage != null) return

            val contentType = metadata.get(Metadata.CONTENT_TYPE) ?: "application/octet-stream"
            val fileName = metadata.get(TikaCoreProperties.RESOURCE_NAME_KEY) ?: "unknown_cover"

            if (contentType.startsWith("image/")) {
                logger.debug { "Potential cover image found: $fileName ($contentType)" }
                coverImage = ExtractedCoverImage(
                    fileName = fileName,
                    contentType = contentType,
                    data = stream.readAllBytes()
                )
            }
        }
    }
}
