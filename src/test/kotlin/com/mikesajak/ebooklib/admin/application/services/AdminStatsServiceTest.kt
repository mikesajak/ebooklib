package com.mikesajak.ebooklib.admin.application.services

import com.mikesajak.ebooklib.author.application.ports.outgoing.AuthorRepositoryPort
import com.mikesajak.ebooklib.book.application.ports.outgoing.BookCoverMetadataRepositoryPort
import com.mikesajak.ebooklib.book.application.ports.outgoing.BookRepositoryPort
import com.mikesajak.ebooklib.book.application.ports.outgoing.EbookFormatFileRepositoryPort
import com.mikesajak.ebooklib.series.application.ports.outgoing.SeriesRepositoryPort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class AdminStatsServiceTest {

    private val bookRepository = mock(BookRepositoryPort::class.java)
    private val authorRepository = mock(AuthorRepositoryPort::class.java)
    private val seriesRepository = mock(SeriesRepositoryPort::class.java)
    private val formatRepository = mock(EbookFormatFileRepositoryPort::class.java)
    private val coverRepository = mock(BookCoverMetadataRepositoryPort::class.java)

    private val adminStatsService = AdminStatsService(
        bookRepository,
        authorRepository,
        seriesRepository,
        formatRepository,
        coverRepository
    )

    @Test
    fun `getStats returns correct aggregated counts`() {
        // given
        `when`(bookRepository.count()).thenReturn(10L)
        `when`(authorRepository.count()).thenReturn(5L)
        `when`(seriesRepository.count()).thenReturn(3L)
        `when`(formatRepository.count()).thenReturn(15L)
        `when`(coverRepository.count()).thenReturn(8L)

        // when
        val stats = adminStatsService.getStats()

        // then
        assertEquals(10L, stats.bookCount)
        assertEquals(5L, stats.authorCount)
        assertEquals(3L, stats.seriesCount)
        assertEquals(15L, stats.formatCount)
        assertEquals(8L, stats.coverCount)
    }
}
