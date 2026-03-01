package com.mikesajak.ebooklib.admin.domain.model

import java.util.*

data class User(
    val id: UUID?,
    val username: String,
    val roles: Set<String>,
    val enabled: Boolean = true
)
