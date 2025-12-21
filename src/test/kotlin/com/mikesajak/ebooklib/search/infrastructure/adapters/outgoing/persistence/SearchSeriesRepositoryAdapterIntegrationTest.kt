package com.mikesajak.ebooklib.search.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.series.infrastructure.adapters.outgoing.persistence.SeriesEntity
import com.mikesajak.ebooklib.series.infrastructure.adapters.outgoing.persistence.SeriesJpaRepository
import com.mikesajak.ebooklib.common.domain.model.PaginationRequest
import com.mikesajak.ebooklib.config.BaseIntegrationTest
import com.mikesajak.ebooklib.file.application.ports.outgoing.FileStoragePort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import software.amazon.awssdk.services.s3.S3Client
import java.util.UUID

@ActiveProfiles("test")
@TestPropertySource(properties = [
    "minio.endpoint=http://localhost:9000",
    "minio.access-key=testaccesskey",
    "minio.secret-key=testsecretkey",
    "minio.bucket-name=test-bucket"
])
class SearchSeriesRepositoryAdapterIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var searchSeriesRepositoryAdapter: SearchSeriesRepositoryAdapter

    @Autowired
    private lateinit var seriesRepository: SeriesJpaRepository

    @MockitoBean
    private lateinit var fileStoragePort: FileStoragePort

    @MockitoBean
    private lateinit var s3Client: S3Client

    private lateinit var series1: SeriesEntity
    private lateinit var series2: SeriesEntity

    @BeforeEach
    fun setUp() {
        seriesRepository.deleteAll()

        series1 = SeriesEntity(id = UUID.randomUUID(), title = "Foundation", description = "Sci-fi series")
        series2 = SeriesEntity(id = UUID.randomUUID(), title = "Dune", description = "Epic space opera")
        seriesRepository.saveAll(listOf(series1, series2))
    }

    @Test
    fun `should search series by title`() {
        // Given
        val query = "title==\"Foundation\""
        val pagination = PaginationRequest(0, 10)

        // When
        val result = searchSeriesRepositoryAdapter.search(query, pagination)

        // Then
        assertEquals(1, result.totalElements)
        assertEquals("Foundation", result.content[0].title)
    }

    @Test
    fun `should search series by title using like`() {
        // Given
        val query = "title=like=\"Found\""
        val pagination = PaginationRequest(0, 10)

        // When
        val result = searchSeriesRepositoryAdapter.search(query, pagination)

        // Then
        assertEquals(1, result.totalElements)
        assertEquals("Foundation", result.content[0].title)
    }
}
