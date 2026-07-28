package com.mikesajak.ebooklib.infrastructure.security.web

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)
