package com.mikesajak.ebooklib.importing.infrastructure.adapters.outgoing.extraction

import com.mikesajak.ebooklib.importing.domain.model.ExtractedCoverImage
import com.mikesajak.ebooklib.importing.domain.model.ExtractedEbookMetadata
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

class MobiEbookMetadataExtractor {

    fun extract(fileContent: InputStream): ExtractedEbookMetadata? {
        val bytes = fileContent.readAllBytes()
        if (bytes.size < 78) {
            logger.warn { "MOBI file content is too small to be a valid PDB header (${bytes.size} bytes)" }
            return null
        }

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)

        // Check PDB header record count
        val numRecords = buffer.getShort(76).toInt() and 0xFFFF
        if (numRecords <= 0 || bytes.size < 78 + numRecords * 8) {
            logger.warn { "Invalid record count in PDB header: $numRecords" }
            return null
        }

        // Read Record offsets
        val recordOffsets = IntArray(numRecords)
        for (i in 0 until numRecords) {
            recordOffsets[i] = buffer.getInt(78 + i * 8)
        }

        val record0Offset = recordOffsets[0]
        if (record0Offset < 0 || record0Offset >= bytes.size) {
            logger.warn { "Invalid Record 0 offset: $record0Offset" }
            return null
        }

        // Check PalmDOC header (16 bytes) + MOBI magic at record0Offset + 16
        if (bytes.size < record0Offset + 16 + 8) {
            logger.warn { "File too small to contain MOBI header" }
            return null
        }

        val mobiMagicOffset = record0Offset + 16
        val mobiMagic = String(bytes, mobiMagicOffset, 4, StandardCharsets.US_ASCII)
        if (mobiMagic != "MOBI") {
            logger.warn { "Invalid MOBI header magic: $mobiMagic" }
            return null
        }

        val mobiHeaderLength = buffer.getInt(mobiMagicOffset + 4)
        val textEncodingCode = buffer.getInt(mobiMagicOffset + 12)
        val charset = if (textEncodingCode == 65001) StandardCharsets.UTF_8 else StandardCharsets.ISO_8859_1

        // Title extraction from MOBI Header
        var title: String? = null
        val fullNameOffsetRel = if (mobiHeaderLength >= 88) buffer.getInt(mobiMagicOffset + 80) else -1
        val fullNameLength = if (mobiHeaderLength >= 88) buffer.getInt(mobiMagicOffset + 84) else -1

        if (fullNameOffsetRel >= 0 && fullNameLength > 0) {
            val titleAbsOffset = record0Offset + fullNameOffsetRel
            if (titleAbsOffset >= 0 && titleAbsOffset + fullNameLength <= bytes.size) {
                title = String(bytes, titleAbsOffset, fullNameLength, charset).trim()
            }
        }

        // Extract EXTH Header if present
        val exthFlags = if (mobiHeaderLength >= 116) buffer.getInt(mobiMagicOffset + 112) else 0
        val hasExth = (exthFlags and 0x40) != 0

        val authors = mutableListOf<String>()
        var publisher: String? = null
        var description: String? = null
        var pubDateStr: String? = null
        var exthTitle: String? = null
        var coverOffset: Int? = null

        val exthHeaderOffset = mobiMagicOffset + mobiHeaderLength
        if (hasExth && bytes.size >= exthHeaderOffset + 12) {
            val exthMagic = String(bytes, exthHeaderOffset, 4, StandardCharsets.US_ASCII)
            if (exthMagic == "EXTH") {
                val exthLength = buffer.getInt(exthHeaderOffset + 4)
                val recordCount = buffer.getInt(exthHeaderOffset + 8)

                var currentPos = exthHeaderOffset + 12
                val exthEnd = (exthHeaderOffset + exthLength).coerceAtMost(bytes.size)

                for (r in 0 until recordCount) {
                    if (currentPos + 8 > exthEnd) break
                    val recordType = buffer.getInt(currentPos)
                    val recordLen = buffer.getInt(currentPos + 4)

                    if (recordLen < 8 || currentPos + recordLen > exthEnd) break

                    val dataLen = recordLen - 8
                    val recordDataOffset = currentPos + 8

                    when (recordType) {
                        100 -> { // Creator / Author
                            val author = String(bytes, recordDataOffset, dataLen, charset).trim()
                            if (author.isNotEmpty()) authors.add(author)
                        }

                        101 -> { // Publisher
                            publisher = String(bytes, recordDataOffset, dataLen, charset).trim()
                        }

                        103 -> { // Description
                            description = String(bytes, recordDataOffset, dataLen, charset).trim()
                        }

                        106 -> { // Publication Date
                            pubDateStr = String(bytes, recordDataOffset, dataLen, charset).trim()
                        }

                        503 -> { // Updated Title / Updated Title from EXTH
                            exthTitle = String(bytes, recordDataOffset, dataLen, charset).trim()
                        }

                        201 -> { // CoverOffset (Index relative to first image record or PDB)
                            if (dataLen == 4) {
                                coverOffset = buffer.getInt(recordDataOffset)
                            }
                        }
                    }

                    currentPos += recordLen
                }
            }
        }

        val finalTitle = (if (!exthTitle.isNullOrBlank()) exthTitle else title)?.ifBlank { null }
        val publicationDate = parseDate(pubDateStr)

        // Attempt cover image extraction
        val coverImage = extractCoverImage(bytes, recordOffsets, mobiMagicOffset, mobiHeaderLength, coverOffset, buffer)

        return ExtractedEbookMetadata(title = finalTitle,
                                      authors = authors,
                                      creationDate = null,
                                      publicationDate = publicationDate,
                                      publisher = publisher,
                                      description = description,
                                      coverImage = coverImage)
    }

    private fun extractCoverImage(bytes: ByteArray,
                                  recordOffsets: IntArray,
                                  mobiMagicOffset: Int,
                                  mobiHeaderLength: Int,
                                  exthCoverOffset: Int?,
                                  buffer: ByteBuffer): ExtractedCoverImage? {
        val firstImageIndex = if (mobiHeaderLength >= 112) buffer.getInt(mobiMagicOffset + 108) else -1

        val targetRecordIndex = when {
                                    exthCoverOffset != null && exthCoverOffset >= 0 -> {
                                        if (firstImageIndex > 0) firstImageIndex + exthCoverOffset else exthCoverOffset
                                    }

                                    firstImageIndex > 0 -> firstImageIndex
                                    else -> null
                                } ?: return null

        if (targetRecordIndex < 0 || targetRecordIndex >= recordOffsets.size) return null

        val startOffset = recordOffsets[targetRecordIndex]
        val endOffset = if (targetRecordIndex + 1 < recordOffsets.size) {
            recordOffsets[targetRecordIndex + 1]
        } else {
            bytes.size
        }

        if (startOffset < 0 || startOffset >= bytes.size || endOffset <= startOffset || endOffset > bytes.size) {
            return null
        }

        val imageBytes = bytes.copyOfRange(startOffset, endOffset)
        if (imageBytes.isEmpty()) return null

        // Detect image type
        val contentType = when {
            imageBytes.size >= 3 && imageBytes[0] == 0xFF.toByte() && imageBytes[1] == 0xD8.toByte() && imageBytes[2] == 0xFF.toByte() -> "image/jpeg"
            imageBytes.size >= 8 && imageBytes[0] == 0x89.toByte() && imageBytes[1] == 'P'.code.toByte() && imageBytes[2] == 'N'.code.toByte() && imageBytes[3] == 'G'.code.toByte() -> "image/png"
            imageBytes.size >= 6 && imageBytes[0] == 'G'.code.toByte() && imageBytes[1] == 'I'.code.toByte() && imageBytes[2] == 'F'.code.toByte() -> "image/gif"
            else -> return null
        }

        val ext = when (contentType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/gif" -> "gif"
            else -> "img"
        }

        return ExtractedCoverImage(fileName = "cover.$ext", contentType = contentType, data = imageBytes)
    }

    private fun parseDate(dateStr: String?): LocalDate? {
        if (dateStr == null) return null
        return try {
            val cleanDate = dateStr.substringBefore("T")
            LocalDate.parse(cleanDate)
        } catch (e: Exception) {
            logger.debug { "Failed to parse date string in MOBI: $dateStr" }
            null
        }
    }
}
