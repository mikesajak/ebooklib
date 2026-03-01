package com.mikesajak.ebooklib.admin.application.services

import com.mikesajak.ebooklib.admin.domain.model.AdminStats
import com.mikesajak.ebooklib.author.application.ports.outgoing.AuthorRepositoryPort
import com.mikesajak.ebooklib.book.application.ports.outgoing.BookCoverMetadataRepositoryPort
import com.mikesajak.ebooklib.book.application.ports.outgoing.BookRepositoryPort
import com.mikesajak.ebooklib.book.application.ports.outgoing.EbookFormatFileRepositoryPort
import com.mikesajak.ebooklib.series.application.ports.outgoing.SeriesRepositoryPort
import org.springframework.stereotype.Service

@Service
class AdminStatsService(
    private val bookRepository: BookRepositoryPort,
    private val authorRepository: AuthorRepositoryPort,
    private val seriesRepository: SeriesRepositoryPort,
    private val formatRepository: EbookFormatFileRepositoryPort,
    private val coverRepository: BookCoverMetadataRepositoryPort
) {
    fun getStats(): AdminStats {
        return AdminStats(
            bookCount = bookRepository.count(),
            authorCount = authorRepository.count(),
            seriesCount = seriesRepository.count(),
            formatCount = formatRepository.count(),
            coverCount = coverRepository.count(),
            totalFormatSize = formatRepository.totalFileSize(),
            totalCoverSize = coverRepository.totalFileSize()
        )
    }
}
