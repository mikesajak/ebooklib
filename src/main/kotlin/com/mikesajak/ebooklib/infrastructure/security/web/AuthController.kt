package com.mikesajak.ebooklib.infrastructure.security.web

import com.mikesajak.ebooklib.infrastructure.security.UserService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class AuthController(
    @Value("\${app.security.enabled:true}") private val securityEnabled: Boolean,
    private val userService: UserService
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

    @PostMapping("/user/change-password")
    fun changePassword(
        @RequestBody request: ChangePasswordRequest,
        authentication: Authentication?
    ): ResponseEntity<Map<String, String>> {
        val username = if (!securityEnabled || authentication == null) "dev-user" else authentication.name
        userService.changePassword(username, request.currentPassword, request.newPassword)
        return ResponseEntity.ok(mapOf("message" to "Password changed successfully"))
    }
}

