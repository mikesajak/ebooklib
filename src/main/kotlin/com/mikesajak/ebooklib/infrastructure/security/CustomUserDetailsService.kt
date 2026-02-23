package com.mikesajak.ebooklib.infrastructure.security

import com.mikesajak.ebooklib.infrastructure.security.persistence.UserJpaRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val userRepository: UserJpaRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByUsername(username)
            ?: throw UsernameNotFoundException("User not found: $username")

        return User.builder()
            .username(user.username)
            .password(user.password)
            .disabled(!user.enabled)
            .authorities(user.roles.map { SimpleGrantedAuthority(it.role) })
            .build()
    }
}
