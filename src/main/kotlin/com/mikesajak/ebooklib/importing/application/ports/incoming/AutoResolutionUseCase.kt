package com.mikesajak.ebooklib.importing.application.ports.incoming

import com.mikesajak.ebooklib.importing.domain.model.ImportSessionId
import com.mikesajak.ebooklib.importing.domain.model.ResolutionItemId
import java.util.*

enum class AutoResolveStrategy {
    TRUST_INCOMING,
    TRUST_EXISTING,
    NEW_ONLY
}

interface AutoResolutionUseCase {
    fun autoResolve(sessionId: ImportSessionId, itemIds: List<ResolutionItemId>? = null, strategy: AutoResolveStrategy)
}
