package com.mikesajak.ebooklib.infrastructure.security.web

import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class AuthController(
    @Value("\${app.security.enabled:true}") private val securityEnabled: Boolean
) {

    @GetMapping("/me")
    fun me(authentication: Authentication?): UserResponse {
        if (!securityEnabled || authentication == null) {
            return UserResponse("dev-user", setOf("ROLE_ADMIN", "ROLE_USER"))
        }

        val username = authentication.name
        val roles = authentication.authorities.map { it.authority }.toSet()

        return UserResponse(username, roles)
    }
}
