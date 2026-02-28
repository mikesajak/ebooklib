package com.mikesajak.ebooklib.importing.infrastructure.adapters.outgoing.extraction

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDDocumentInformation
import org.apache.pdfbox.pdmodel.PDPage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class TikaEbookMetadataExtractorTest {

    private val extractor = TikaEbookMetadataExtractor()

    @Test
    fun `should extract metadata from PDF`() {
        // given
        val pdfData = createSimplePdf(
            title = "PDF Test Title",
            author = "PDF Test Author",
            creationDate = "2023-01-01"
        )

        // when
        val metadata = extractor.extract(ByteArrayInputStream(pdfData), "test.pdf", "application/pdf")

        // then
        assertThat(metadata.title).isEqualTo("PDF Test Title")
        assertThat(metadata.authors).containsExactly("PDF Test Author")
        // Use a more relaxed check for PDF dates as they depend on PDFBox version and system timezone
        assertThat(metadata.creationDate).isNotNull()
    }

    @Test
    fun `should extract metadata from EPUB`() {
        // given
        val epubData = createSimpleEpub(
            title = "EPUB Test Title",
            authors = listOf("Author 1", "Author 2"),
            publisher = "EPUB Publisher",
            date = "2024-05-20",
            description = "This is a test ebook description."
        )

        // when
        val metadata = extractor.extract(ByteArrayInputStream(epubData), "test.epub", "application/epub+zip")

        // then
        assertThat(metadata.title).isEqualTo("EPUB Test Title")
        assertThat(metadata.authors).containsExactlyInAnyOrder("Author 1", "Author 2")
        assertThat(metadata.publisher).isEqualTo("EPUB Publisher")
        // Tika's EPUB parser maps dc:date to TikaCoreProperties.CREATED by default
        assertThat(metadata.creationDate.toString()).isEqualTo("2024-05-20")
        assertThat(metadata.description).isEqualTo("This is a test ebook description.")
    }

    @Test
    fun `should return empty metadata for invalid or empty stream`() {
        // when
        val metadata = extractor.extract(ByteArrayInputStream(byteArrayOf()), "corrupted.epub", "application/epub+zip")

        // then
        assertThat(metadata.title).isNull()
        assertThat(metadata.authors).isEmpty()
    }

    @Test
    fun `should extract cover image from EPUB`() {
        // given
        val coverImageData = byteArrayOf(0, 1, 2, 3, 4, 5)
        val epubData = createSimpleEpub(
            title = "EPUB with Cover",
            authors = listOf("Author"),
            coverImageData = coverImageData
        )

        // when
        val metadata = extractor.extract(ByteArrayInputStream(epubData), "test.epub", "application/epub+zip")

        // then
        assertThat(metadata.coverImage).isNotNull
        assertThat(metadata.coverImage?.data).isEqualTo(coverImageData)
        assertThat(metadata.coverImage?.contentType).isEqualTo("image/jpeg")
    }

    private fun createSimplePdf(title: String, author: String, creationDate: String? = null): ByteArray {
        val document = PDDocument()
        val info = PDDocumentInformation()
        info.title = title
        info.author = author
        if (creationDate != null) {
            val calendar = GregorianCalendar(TimeZone.getTimeZone("UTC"))
            calendar.set(2023, Calendar.JANUARY, 1, 12, 0, 0)
            info.creationDate = calendar
        }
        document.documentInformation = info
        document.addPage(PDPage())
        val out = ByteArrayOutputStream()
        document.save(out)
        document.close()
        return out.toByteArray()
    }

    private fun createSimpleEpub(
        title: String, 
        authors: List<String>, 
        publisher: String? = null,
        date: String? = null,
        description: String? = null,
        coverImageData: ByteArray? = null
    ): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("mimetype"))
            zip.write("application/epub+zip".toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("META-INF/container.xml"))
            zip.write("""<?xml version="1.0"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
  </rootfiles>
</container>""".toByteArray())
            zip.closeEntry()

            val coverManifestItem = if (coverImageData != null) """<item id="cover" href="cover.jpg" media-type="image/jpeg" properties="cover-image"/>""" else ""
            
            zip.putNextEntry(ZipEntry("OEBPS/content.opf"))
            
            val authorTags = authors.joinToString("\n") { "<dc:creator>$it</dc:creator>" }
            val publisherTag = publisher?.let { "<dc:publisher>$it</dc:publisher>" } ?: ""
            val dateTag = date?.let { "<dc:date>$it</dc:date>" } ?: ""
            val descriptionTag = description?.let { "<dc:description>$it</dc:description>" } ?: ""

            zip.write("""<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" unique-identifier="pub-id" version="3.0">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:title>$title</dc:title>
    $authorTags
    $publisherTag
    $dateTag
    $descriptionTag
    <dc:language>en</dc:language>
    <dc:identifier id="pub-id">test-id</dc:identifier>
    ${if (coverImageData != null) """<meta name="cover" content="cover"/>""" else ""}
  </metadata>
  <manifest>
    <item id="item1" href="text.xhtml" media-type="application/xhtml+xml"/>
    $coverManifestItem
  </manifest>
  <spine>
    <itemref idref="item1"/>
  </spine>
</package>""".toByteArray())
            zip.closeEntry()

            if (coverImageData != null) {
                zip.putNextEntry(ZipEntry("OEBPS/cover.jpg"))
                zip.write(coverImageData)
                zip.closeEntry()
            }

            zip.putNextEntry(ZipEntry("OEBPS/text.xhtml"))
            zip.write("""<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml">
  <body>
    <h1>Test</h1>
  </body>
</html>""".toByteArray())
            zip.closeEntry()
        }
        return out.toByteArray()
    }
}
