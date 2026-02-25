package com.mikesajak.ebooklib.infrastructure.security

import com.mikesajak.ebooklib.infrastructure.security.persistence.UserJpaRepository
import mu.KotlinLogging
import org.slf4j.LoggerFactory
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class CustomUserDetailsService(
    private val userRepository: UserJpaRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        logger.debug("Loading user by username: {}", username)
        val user = userRepository.findByUsername(username)
            ?: run {
                logger.error("User not found: {}", username)
                throw UsernameNotFoundException("User not found: $username")
            }

        logger.debug("User found: {}, enabled: {}, roles: {}", user.username, user.enabled, user.roles.map { it.role })

        return User.builder()
            .username(user.username)
            .password(user.password)
            .disabled(!user.enabled)
            .authorities(user.roles.map { SimpleGrantedAuthority(it.role) })
            .build()
    }
}
