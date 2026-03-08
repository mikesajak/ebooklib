package com.mikesajak.ebooklib.importing.domain.model

data class SupportedEbookFormat(
    val extension: String,
    val mimeType: String,
    val description: String
)

object EbookFormats {
    val SUPPORTED_FORMATS = listOf(
        SupportedEbookFormat("epub", "application/epub+zip", "Electronic Publication"),
        SupportedEbookFormat("pdf", "application/pdf", "Portable Document Format"),
        SupportedEbookFormat("mobi", "application/x-mobipocket-ebook", "Mobipocket Ebook"),
        SupportedEbookFormat("azw3", "application/vnd.amazon.mobi8-ebook", "Kindle Format 8")
    )

    fun isSupportedExtension(extension: String): Boolean =
        SUPPORTED_FORMATS.any { it.extension.equals(extension, ignoreCase = true) }

    fun isSupportedMimeType(mimeType: String): Boolean =
        SUPPORTED_FORMATS.any { it.mimeType.equals(mimeType, ignoreCase = true) }
}
