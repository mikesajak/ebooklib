package com.mikesajak.ebooklib.importing.application.ports.incoming

import com.mikesajak.ebooklib.importing.domain.model.ImportSession
import com.mikesajak.ebooklib.importing.domain.model.ImportSessionId

interface ImportSessionUseCase {
    fun createSession(totalFiles: Int): ImportSession
    fun getSession(id: ImportSessionId): ImportSession?
    fun getActiveSessions(): List<ImportSession>
    fun updateProgress(id: ImportSessionId, processed: Int, failed: Int): ImportSession
    fun finalizeSession(id: ImportSessionId): ImportSession
    fun cancelSession(id: ImportSessionId): ImportSession
    fun deleteSession(id: ImportSessionId)
}
