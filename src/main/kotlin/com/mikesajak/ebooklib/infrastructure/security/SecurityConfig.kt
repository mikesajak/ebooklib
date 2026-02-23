package com.mikesajak.ebooklib.infrastructure.security

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    @ConditionalOnProperty(name = ["app.security.enabled"], havingValue = "true", matchIfMissing = true)
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            authorizeHttpRequests {
                authorize("/", permitAll)
                authorize("/index.html", permitAll)
                authorize("/static/**", permitAll)
                authorize("/assets/**", permitAll)
                authorize("/favicon.ico", permitAll)
                authorize("/locales/**", permitAll)
                authorize("/api/**", authenticated)
                authorize(anyRequest, permitAll)
            }
            formLogin { }
            httpBasic { }
            csrf { disable() } // TEMPORARY: Will be enabled and configured in SEC-004b
        }

        return http.build()
    }

    @Bean
    @ConditionalOnProperty(name = ["app.security.enabled"], havingValue = "false")
    fun permitAllFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            authorizeHttpRequests {
                authorize(anyRequest, permitAll)
            }
            csrf { disable() }
        }
        return http.build()
    }
}
