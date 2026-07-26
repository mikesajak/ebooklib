package com.mikesajak.ebooklib.importing.infrastructure.adapters.outgoing.extraction

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

class MobiEbookMetadataExtractorTest {

    private val extractor = MobiEbookMetadataExtractor()
    private val tikaExtractor = TikaEbookMetadataExtractor()

    @Test
    fun `should extract title author publisher and dates from MOBI binary data`() { // given
        val title = "The Great MOBI Adventure"
        val author = "Jane Doe"
        val publisher = "MOBI Press"
        val description = "A test description for MOBI"
        val mobiBytes = createSyntheticMobi(title = title, author = author, publisher = publisher, description = description)

        // when
        val metadata = extractor.extract(ByteArrayInputStream(mobiBytes))

        // then
        assertThat(metadata).isNotNull
        assertThat(metadata!!.title).isEqualTo(title)
        assertThat(metadata.authors).containsExactly(author)
        assertThat(metadata.publisher).isEqualTo(publisher)
        assertThat(metadata.description).isEqualTo(description)
    }

    @Test
    fun `should extract MOBI metadata via TikaEbookMetadataExtractor`() { // given
        val title = "Integrated MOBI Book"
        val author = "John Smith"
        val mobiBytes = createSyntheticMobi(title = title, author = author)

        // when
        val metadata = tikaExtractor.extract(ByteArrayInputStream(mobiBytes), "book.mobi", "application/x-mobipocket-ebook")

        // then
        assertThat(metadata.title).isEqualTo(title)
        assertThat(metadata.authors).containsExactly(author)
    }

    @Test
    fun `should return null for non-MOBI file`() { // given
        val invalidBytes = "This is not a MOBI file".toByteArray(StandardCharsets.UTF_8)

        // when
        val metadata = extractor.extract(ByteArrayInputStream(invalidBytes))

        // then
        assertThat(metadata).isNull()
    }

    private fun createSyntheticMobi(title: String,
                                    author: String? = null,
                                    publisher: String? = null,
                                    description: String? = null): ByteArray {
        val out = ByteArrayOutputStream()

        // EXTH Records
        val exthRecordsStream = ByteArrayOutputStream()
        var exthRecordCount = 0

        fun addExthRecord(type: Int, value: String) {
            val valBytes = value.toByteArray(StandardCharsets.UTF_8)
            val recordLen = 8 + valBytes.size
            val buf = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
            buf.putInt(type)
            buf.putInt(recordLen)
            exthRecordsStream.write(buf.array())
            exthRecordsStream.write(valBytes)
            exthRecordCount++
        }

        if (author != null) addExthRecord(100, author)
        if (publisher != null) addExthRecord(101, publisher)
        if (description != null) addExthRecord(103, description)

        val exthRecordsBytes = exthRecordsStream.toByteArray()
        val exthHeaderLen = 12 + exthRecordsBytes.size
        val exthHeaderBuf = ByteBuffer.allocate(12).order(ByteOrder.BIG_ENDIAN)
        exthHeaderBuf.put("EXTH".toByteArray(StandardCharsets.US_ASCII))
        exthHeaderBuf.putInt(exthHeaderLen)
        exthHeaderBuf.putInt(exthRecordCount)

        val exthFullBlock = ByteArrayOutputStream()
        exthFullBlock.write(exthHeaderBuf.array())
        exthFullBlock.write(exthRecordsBytes)

        val titleBytes = title.toByteArray(StandardCharsets.UTF_8)

        // MOBI Header (length 232)
        val mobiHeaderLen = 232
        val mobiBuf = ByteBuffer.allocate(mobiHeaderLen).order(ByteOrder.BIG_ENDIAN)
        mobiBuf.put("MOBI".toByteArray(StandardCharsets.US_ASCII))
        mobiBuf.putInt(mobiHeaderLen)
        mobiBuf.putInt(2) // MOBI type
        mobiBuf.putInt(65001) // Text encoding UTF-8

        // Fill up to index 80
        mobiBuf.position(80) // Full name offset (relative to Record 0 start = 16 (PalmDOC) + 232 (MOBI Header) + exthFullBlock.size)
        val fullNameOffsetRel = 16 + mobiHeaderLen + exthFullBlock.size()
        mobiBuf.putInt(fullNameOffsetRel)
        mobiBuf.putInt(titleBytes.size)

        // EXTH flags at 112
        mobiBuf.position(112)
        mobiBuf.putInt(0x40) // Has EXTH

        mobiBuf.position(mobiHeaderLen) // Position at end of MOBI header

        // Record 0 payload: PalmDOC (16 bytes) + MOBI Header (232 bytes) + EXTH block + Title bytes
        val palmDocBuf = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN)
        palmDocBuf.putShort(1) // compression
        palmDocBuf.putShort(0)
        palmDocBuf.putInt(1000)
        palmDocBuf.putShort(1)
        palmDocBuf.putShort(4096)
        palmDocBuf.putShort(0)
        palmDocBuf.putShort(0)

        val record0Out = ByteArrayOutputStream()
        record0Out.write(palmDocBuf.array())
        record0Out.write(mobiBuf.array())
        record0Out.write(exthFullBlock.toByteArray())
        record0Out.write(titleBytes)

        val record0Bytes = record0Out.toByteArray()

        // PDB Header: 78 bytes. Number of records at byte 76 = 1.
        val numRecords = 1
        val record0Offset = 78 + numRecords * 8

        val pdbBuf = ByteBuffer.allocate(record0Offset).order(ByteOrder.BIG_ENDIAN) // fill name
        pdbBuf.put("TestDatabaseName                ".toByteArray(StandardCharsets.US_ASCII))
        pdbBuf.position(60)
        pdbBuf.put("BOOK".toByteArray(StandardCharsets.US_ASCII))
        pdbBuf.put("MOBI".toByteArray(StandardCharsets.US_ASCII))
        pdbBuf.position(76)
        pdbBuf.putShort(numRecords.toShort())

        // Record 0 Header entry at offset 78: offset (4 bytes) + attr (1 byte) + uid (3 bytes)
        pdbBuf.putInt(record0Offset)
        pdbBuf.put(0)
        pdbBuf.put(byteArrayOf(0, 0, 1))

        out.write(pdbBuf.array())
        out.write(record0Bytes)

        return out.toByteArray()
    }
}
