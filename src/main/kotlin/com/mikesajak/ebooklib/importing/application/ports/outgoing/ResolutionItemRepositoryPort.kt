package com.mikesajak.ebooklib.importing.application.ports.outgoing

import com.mikesajak.ebooklib.importing.domain.model.ImportSessionId
import com.mikesajak.ebooklib.importing.domain.model.ResolutionItem
import com.mikesajak.ebooklib.importing.domain.model.ResolutionItemId

interface ResolutionItemRepositoryPort {
    fun save(resolutionItem: ResolutionItem): ResolutionItem
    fun findById(id: ResolutionItemId): ResolutionItem?
    fun findByImportSessionIdAndTitleAndAuthors(importSessionId: ImportSessionId, title: String, authors: List<String>): ResolutionItem?
    fun findByImportSessionId(importSessionId: ImportSessionId): List<ResolutionItem>
    fun delete(id: ResolutionItemId)
    fun deleteByImportSessionId(importSessionId: ImportSessionId)
}
