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
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.LocalDate
import java.util.*
import javax.imageio.ImageIO

private val logger = KotlinLogging.logger {}

@Component
class TikaEbookMetadataExtractor : EbookMetadataExtractorUseCase {

    private val mobiExtractor = MobiEbookMetadataExtractor()

    override fun extract(fileContent: InputStream, fileName: String, contentType: String): ExtractedEbookMetadata {
        logger.info { "Extracting metadata from file: $fileName ($contentType)" }

        val fileBytes = fileContent.readAllBytes()

        if (isMobiFormat(fileName, contentType)) {
            try {
                val mobiMetadata = mobiExtractor.extract(ByteArrayInputStream(fileBytes))
                if (mobiMetadata != null && (mobiMetadata.title != null || mobiMetadata.authors.isNotEmpty())) {
                    logger.info { "Successfully extracted MOBI metadata for $fileName: title='${mobiMetadata.title}', authors=${mobiMetadata.authors}" }
                    return mobiMetadata
                }
            } catch (e: Exception) {
                logger.warn(e) { "Native MOBI extractor failed for $fileName, falling back to Tika" }
            }
        }

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
            parser.parse(ByteArrayInputStream(fileBytes), handler, metadata, context)
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

    private fun isMobiFormat(fileName: String, contentType: String): Boolean {
        val lowerName = fileName.lowercase(Locale.getDefault())
        val lowerType = contentType.lowercase(Locale.getDefault())
        return lowerType == "application/x-mobipocket-ebook" || lowerType == "application/vnd.amazon.mobi8-ebook" || lowerName.endsWith(".mobi") || lowerName.endsWith(
            ".azw") || lowerName.endsWith(".azw3")
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
        private val candidateImages = mutableListOf<ExtractedCandidateImage>()

        val coverImage: ExtractedCoverImage?
            get() = selectBestCoverImage()

        private data class ExtractedCandidateImage(
            val fileName: String,
            val contentType: String,
            val data: ByteArray,
            val width: Int,
            val height: Int
        ) {
            val pixelCount: Long = width.toLong() * height.toLong()
            val isExplicitCoverName: Boolean = fileName.contains("cover", ignoreCase = true)
        }

        override fun shouldParseEmbedded(metadata: Metadata): Boolean {
            val contentType = metadata.get(Metadata.CONTENT_TYPE)
            val fileName = metadata.get(TikaCoreProperties.RESOURCE_NAME_KEY) ?: ""
            return contentType?.startsWith("image/") ?: false || fileName.contains("cover", ignoreCase = true)
        }

        override fun parseEmbedded(stream: InputStream, handler: ContentHandler, metadata: Metadata, outputHtml: Boolean) {
            val contentType = metadata.get(Metadata.CONTENT_TYPE) ?: "application/octet-stream"
            val fileName = metadata.get(TikaCoreProperties.RESOURCE_NAME_KEY) ?: "unknown_cover"

            if (contentType.startsWith("image/")) {
                try {
                    val data = stream.readAllBytes()
                    if (data.isEmpty()) return

                    var width = 0
                    var height = 0
                    try {
                        val bufferedImage = ImageIO.read(ByteArrayInputStream(data))
                        if (bufferedImage != null) {
                            width = bufferedImage.width
                            height = bufferedImage.height
                        }
                    } catch (e: Exception) {
                        logger.debug { "Failed to read image dimensions for $fileName: ${e.message}" }
                    }

                    logger.debug { "Found embedded candidate image: $fileName ($contentType, ${data.size} bytes, ${width}x${height})" }
                    candidateImages.add(
                        ExtractedCandidateImage(
                            fileName = fileName,
                            contentType = contentType,
                            data = data,
                            width = width,
                            height = height
                        )
                    )
                } catch (e: Exception) {
                    logger.warn(e) { "Failed to read embedded image data for $fileName" }
                }
            }
        }

        private fun selectBestCoverImage(): ExtractedCoverImage? {
            if (candidateImages.isEmpty()) return null

            val bestCandidate = candidateImages.maxWithOrNull(
                compareBy<ExtractedCandidateImage> { it.isExplicitCoverName }
                    .thenBy { it.pixelCount }
                    .thenBy { it.data.size }
            ) ?: return null

            logger.info { "Selected best cover image candidate: ${bestCandidate.fileName} (${bestCandidate.contentType}, ${bestCandidate.data.size} bytes, ${bestCandidate.width}x${bestCandidate.height})" }

            return ExtractedCoverImage(
                fileName = bestCandidate.fileName,
                contentType = bestCandidate.contentType,
                data = bestCandidate.data
            )
        }
    }
}
