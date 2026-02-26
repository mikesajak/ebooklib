package com.mikesajak.ebooklib.importing.infrastructure.adapters.outgoing.extraction

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDDocumentInformation
import org.apache.pdfbox.pdmodel.PDPage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class TikaEbookMetadataExtractorTest {

    private val extractor = TikaEbookMetadataExtractor()

    @Test
    fun `should extract metadata from PDF`() {
        // given
        val pdfData = createSimplePdf(
            title = "PDF Test Title",
            author = "PDF Test Author"
        )

        // when
        val metadata = extractor.extract(ByteArrayInputStream(pdfData), "test.pdf", "application/pdf")

        // then
        assertThat(metadata.title).isEqualTo("PDF Test Title")
        assertThat(metadata.authors).containsExactly("PDF Test Author")
    }

    @Test
    fun `should extract metadata from EPUB`() {
        // given
        val epubData = createSimpleEpub(
            title = "EPUB Test Title",
            author = "EPUB Test Author"
        )

        // when
        val metadata = extractor.extract(ByteArrayInputStream(epubData), "test.epub", "application/epub+zip")

        // then
        assertThat(metadata.title).isEqualTo("EPUB Test Title")
        assertThat(metadata.authors).containsExactly("EPUB Test Author")
    }

    @Test
    fun `should extract cover image from EPUB`() {
        // given
        val coverImageData = byteArrayOf(0, 1, 2, 3, 4, 5)
        val epubData = createSimpleEpub(
            title = "EPUB with Cover",
            author = "Author",
            coverImageData = coverImageData
        )

        // when
        val metadata = extractor.extract(ByteArrayInputStream(epubData), "test.epub", "application/epub+zip")

        // then
        assertThat(metadata.coverImage).isNotNull
        assertThat(metadata.coverImage?.data).isEqualTo(coverImageData)
        assertThat(metadata.coverImage?.contentType).isEqualTo("image/jpeg")
    }

    private fun createSimplePdf(title: String, author: String): ByteArray {
        val document = PDDocument()
        val info = PDDocumentInformation()
        info.title = title
        info.author = author
        document.documentInformation = info
        document.addPage(PDPage())
        val out = ByteArrayOutputStream()
        document.save(out)
        document.close()
        return out.toByteArray()
    }

    private fun createSimpleEpub(title: String, author: String, coverImageData: ByteArray? = null): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("mimetype"))
            zip.write("application/epub+zip".toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("META-INF/container.xml"))
            zip.write("""
                <?xml version="1.0"?>
                <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                  <rootfiles>
                    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
                  </rootfiles>
                </container>
            """.trimIndent().toByteArray())
            zip.closeEntry()

            val coverManifestItem = if (coverImageData != null) """<item id="cover" href="cover.jpg" media-type="image/jpeg" properties="cover-image"/>""" else ""
            
            zip.putNextEntry(ZipEntry("OEBPS/content.opf"))
            zip.write("""
                <?xml version="1.0" encoding="UTF-8"?>
                <package xmlns="http://www.idpf.org/2007/opf" unique-identifier="pub-id" version="3.0">
                  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                    <dc:title>$title</dc:title>
                    <dc:creator>$author</dc:creator>
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
                </package>
            """.trimIndent().toByteArray())
            zip.closeEntry()

            if (coverImageData != null) {
                zip.putNextEntry(ZipEntry("OEBPS/cover.jpg"))
                zip.write(coverImageData)
                zip.closeEntry()
            }

            zip.putNextEntry(ZipEntry("OEBPS/text.xhtml"))
            zip.write("""
                <?xml version="1.0" encoding="UTF-8"?>
                <html xmlns="http://www.w3.org/1999/xhtml">
                  <body>
                    <h1>Test</h1>
                  </body>
                </html>
            """.trimIndent().toByteArray())
            zip.closeEntry()
        }
        return out.toByteArray()
    }
}
