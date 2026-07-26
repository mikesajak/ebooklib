package com.mikesajak.ebooklib.importing.domain.model

object ImportUtils {
    private val UNLIKELY_PLACEHOLDERS = setOf(
        "untitled", "cover", "document", "unknown", "page 1", "chapter 1",
        "table of contents", "index", "no title", "default"
    )

    fun isUnlikelyTitle(title: String?): Boolean {
        if (title.isNullOrBlank()) return true
        val clean = title.trim().lowercase()
        if (clean.length <= 1) return true
        if (clean.all { it.isDigit() }) return true
        return UNLIKELY_PLACEHOLDERS.contains(clean)
    }

    fun extractTitleFromFileName(fileName: String): String {
        val nameOnly = fileName.substringAfterLast('/').substringAfterLast('\\')
        return nameOnly.substringBeforeLast('.')
    }
}

