package com.mikesajak.ebooklib.importing.application.ports.outgoing

import com.mikesajak.ebooklib.importing.domain.model.ImportSession
import com.mikesajak.ebooklib.importing.domain.model.ImportSessionId
import java.time.Instant

interface ImportSessionRepositoryPort {
    fun save(importSession: ImportSession): ImportSession
    fun findById(id: ImportSessionId): ImportSession?
    fun findAllByStatus(status: com.mikesajak.ebooklib.importing.domain.model.ImportSessionStatus): List<ImportSession>
    fun findAllExpired(now: Instant): List<ImportSession>
    fun delete(id: ImportSessionId)
    fun incrementProcessed(id: ImportSessionId)
    fun incrementFailed(id: ImportSessionId)
}
