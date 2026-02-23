package com.mikesajak.ebooklib.infrastructure.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
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
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("/", "/index.html", "/static/**", "/assets/**", "/favicon.ico", "/locales/**").permitAll()
                    .requestMatchers("/api/me").permitAll() // TEMPORARY for next task testing
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().permitAll()
            }
            .formLogin { form ->
                form.permitAll()
            }
            .httpBasic { }
            .csrf { csrf ->
                csrf.disable() // TEMPORARY: Will be enabled and configured in SEC-004b
            }

        return http.build()
    }
}
