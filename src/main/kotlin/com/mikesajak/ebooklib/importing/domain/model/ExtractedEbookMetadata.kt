package com.mikesajak.ebooklib.importing.domain.model

import java.time.LocalDate

data class ExtractedEbookMetadata(
    val title: String?,
    val authors: List<String> = emptyList(),
    val creationDate: LocalDate?,
    val publicationDate: LocalDate?,
    val publisher: String?,
    val description: String?,
    val coverImage: ExtractedCoverImage? = null
)

data class ExtractedCoverImage(
    val fileName: String,
    val contentType: String,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ExtractedCoverImage
        if (fileName != other.fileName) return false
        if (contentType != other.contentType) return false
        if (!data.contentEquals(other.data)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}
