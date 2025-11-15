package com.mikesajak.ebooklib.file.application.ports.outgoing

import java.io.InputStream

data class FileMetadata(
        val id: String,
        val fileName: String,
        val contentType: String,
        val size: Long
)

interface FileStoragePort {
    fun uploadFile(fileContent: InputStream, originalFileName: String, contentType: String): FileMetadata
    fun downloadFile(fileId: String): InputStream
    fun deleteFile(fileId: String)
    fun getFileMetadata(fileId: String): FileMetadata?
}
