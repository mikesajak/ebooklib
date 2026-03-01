package com.mikesajak.ebooklib.admin.application.ports.incoming

import java.util.*

data class CreateUserCommand(
    val username: String,
    val roles: Set<String>
)

data class CreatedUserResponse(
    val id: UUID,
    val username: String,
    val initialPassword: String
)
