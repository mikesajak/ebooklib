package com.mikesajak.ebooklib.infrastructure.security.web

data class UserResponse(
    val username: String,
    val roles: Set<String>
)
