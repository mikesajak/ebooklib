package com.mikesajak.ebooklib.admin.infrastructure.incoming.web

import java.util.*

data class UserDto(
    val id: UUID?,
    val username: String,
    val roles: Set<String>,
    val enabled: Boolean
)
