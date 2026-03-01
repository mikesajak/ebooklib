package com.mikesajak.ebooklib.admin.infrastructure.incoming.web

import com.mikesajak.ebooklib.admin.application.services.AdminStatsService
import com.mikesajak.ebooklib.admin.domain.model.AdminStats
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin")
class AdminController(private val adminStatsService: AdminStatsService) {

    @GetMapping("/stats")
    fun getStats(): AdminStatsDto {
        return adminStatsService.getStats().toDto()
    }

    private fun AdminStats.toDto() = AdminStatsDto(
        bookCount = bookCount,
        authorCount = authorCount,
        seriesCount = seriesCount,
        formatCount = formatCount,
        coverCount = coverCount
    )
}
