package com.mikesajak.ebooklib.infrastructure.security

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.filter.OncePerRequestFilter
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

@Configuration
@EnableWebSecurity
class SecurityConfig(private val userDetailsService: CustomUserDetailsService? = null) {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    @ConditionalOnProperty(name = ["app.security.enabled"], havingValue = "true", matchIfMissing = true)
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val requestHandler = CsrfTokenRequestAttributeHandler()
        // set the name of the attribute the CsrfToken will be populated on
        requestHandler.setCsrfRequestAttributeName(null)

        userDetailsService?.let { http.userDetailsService(it) }

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
            formLogin {
                loginPage = "/login"
                authenticationSuccessHandler = { _, response, _ ->
                    response.status = HttpStatus.OK.value()
                }
                authenticationFailureHandler = { _, response, _ ->
                    response.status = HttpStatus.UNAUTHORIZED.value()
                }
            }
            logout {
                logoutUrl = "/logout"
                logoutSuccessHandler = { _, response, _ ->
                    response.status = HttpStatus.OK.value()
                }
            }
            exceptionHandling {
                authenticationEntryPoint = HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
            }
            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.IF_REQUIRED
            }
            csrf {
                csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse()
                csrfTokenRequestHandler = requestHandler
            }
            addFilterAfter<org.springframework.security.web.csrf.CsrfFilter>(CsrfCookieFilter())
        }

        return http.build()
    }

    /**
     * Filter to ensure CSRF token is loaded and sent as a cookie.
     * Required for SPAs where the token is deferred by default in Spring Security 6.
     */
    class CsrfCookieFilter : OncePerRequestFilter() {
        override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
            val csrfToken = request.getAttribute("_csrf") as? CsrfToken
            csrfToken?.token
            filterChain.doFilter(request, response)
        }
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
