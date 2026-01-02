package com.mikesajak.ebooklib.infrastructure.web

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.lang.System.currentTimeMillis
import java.util.*

@ConfigurationProperties("app.request-logging")
data class RequestLoggingProperties(
        val enabled: Boolean = true,
        val logHeaders: Boolean = false,
        val maskedHeaders: Set<String> = setOf("authorization", "proxy-authorization")
)

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@EnableConfigurationProperties(RequestLoggingProperties::class)
class RequestLoggingFilter(private val requestLoggingProperties: RequestLoggingProperties) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(request: HttpServletRequest,
                                  response: HttpServletResponse,
                                  filterChain: FilterChain
    ) {
        if (!requestLoggingProperties.enabled) {
            filterChain.doFilter(request, response)
            return
        }

        val startTime = currentTimeMillis()

        filterChain.doFilter(request, response)

        val duration = currentTimeMillis() - startTime

        val requestHeaders = if (requestLoggingProperties.logHeaders) formatHeaders(request) else "disabled"

        val responseHeaders = if (requestLoggingProperties.logHeaders) formatResponseHeaders(response) else "disabled"

        log.debug("Request: [method={}, uri={}, query={}, status={}, duration={}ms, requestHeaders={}, responseHeaders={}]",
                  request.method, request.requestURI, request.queryString, response.status, duration, requestHeaders, responseHeaders)
    }

    private fun formatResponseHeaders(response: HttpServletResponse): String {
        return response.headerNames
                .joinToString(", ") { headerName -> "$headerName=${getResponseHeaderValue(headerName, response)}" }
    }

    private fun getResponseHeaderValue(headerName: String, response: HttpServletResponse): String? =
        if (shouldMask(headerName)) "***"
        else response.getHeader(headerName)

    private fun formatHeaders(request: HttpServletRequest): String {
        return Collections.list(request.headerNames)
                .joinToString(", ") { headerName -> "$headerName=${getHeaderValue(headerName, request)}" }
    }

    private fun getHeaderValue(headerName: String, request: HttpServletRequest): String? =
        if (shouldMask(headerName)) "***"
        else request.getHeader(headerName)

    private fun shouldMask(headerName: String): Boolean =
        requestLoggingProperties.maskedHeaders.contains(headerName.lowercase(Locale.getDefault()))
}
