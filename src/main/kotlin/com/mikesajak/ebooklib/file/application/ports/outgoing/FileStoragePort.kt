package com.mikesajak.ebooklib.file.application.ports.outgoing

import java.io.InputStream

data class FileMetadata(
        val id: String,
        val fileName: String,
        val contentType: String,
        val size: Long
)

data class FileEntry(
    val key: String,
    val size: Long
)

interface FileStoragePort {
    fun uploadFile(fileContent: InputStream, originalFileName: String, contentType: String, folder: String? = null): FileMetadata
    fun downloadFile(fileId: String): InputStream
    fun deleteFile(fileId: String)
    fun getFileMetadata(fileId: String): FileMetadata?
    fun moveFile(fileId: String, newFolder: String? = null): FileMetadata
    fun listAllFiles(prefix: String? = null): Sequence<FileEntry>
}
