package com.mikesajak.ebooklib.infrastructure.config

import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
    }
}

@Controller
class SpaController {

    @GetMapping("/{path:(?!api)[^\\.]*}")
    fun spa(@PathVariable path: String): String {
        return "forward:/index.html"
    }

    @GetMapping("/{path:(?!api)[^\\.]*}/{other:[^.]*}")
    fun spaNested(@PathVariable path: String, @PathVariable other: String): String {
        return "forward:/index.html"
    }
}
