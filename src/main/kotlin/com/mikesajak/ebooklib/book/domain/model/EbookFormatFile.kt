package com.mikesajak.ebooklib.book.domain.model

import com.mikesajak.ebooklib.file.application.ports.outgoing.FileMetadata
import java.util.*

data class EbookFormatFile(
    val id: UUID,
    val bookId: BookId,
    val fileName: String,
    val contentType: String,
    val fileSize: Long,
    val formatType: String,
    val storageKey: String
) {
    fun toFileMetadata(): FileMetadata {
        return FileMetadata(id.toString(), fileName, contentType, fileSize)
    }
}
