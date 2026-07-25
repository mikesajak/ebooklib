package com.mikesajak.ebooklib.importing.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUpload
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadId
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadStatus
import com.mikesajak.ebooklib.config.TestcontainersConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = [TestcontainersConfig::class])
@Import(StagedEbookUploadEntityMapper::class, StagedEbookUploadRepositoryAdapter::class, JacksonAutoConfiguration::class)
class StagedEbookUploadRepositoryAdapterIntegrationTest {

    @Autowired
    private lateinit var repositoryAdapter: StagedEbookUploadRepositoryAdapter

    @Autowired
    private lateinit var jpaRepository: StagedEbookUploadJpaRepository

    @BeforeEach
    fun setup() {
        jpaRepository.deleteAll()
    }

    @Test
    fun `should save and find staged upload`() {
        // given
        val id = StagedEbookUploadId(UUID.randomUUID())
        val now = Instant.now().truncatedTo(ChronoUnit.MICROS)
        val expiry = now.plus(24, ChronoUnit.HOURS)
        val stagedUpload = StagedEbookUpload(
            id = id,
            fileName = "test.epub",
            contentType = "application/epub+zip",
            fileSize = 1024L,
            metadataJson = """{"title": "Test Book"}""",
            status = StagedEbookUploadStatus.STAGED,
            createdAt = now,
            expiryAt = expiry
        )

        // when
        repositoryAdapter.save(stagedUpload)

        // then
        val found = repositoryAdapter.findById(id)
        assertThat(found).isNotNull
        assertThat(found!!.id).isEqualTo(id)
        assertThat(found.fileName).isEqualTo("test.epub")
        assertThat(found.contentType).isEqualTo("application/epub+zip")
        assertThat(found.fileSize).isEqualTo(1024L)
        assertThat(found.metadataJson).isEqualTo("""{"title": "Test Book"}""")
        assertThat(found.status).isEqualTo(StagedEbookUploadStatus.STAGED)
        assertThat(found.createdAt).isEqualTo(now)
        assertThat(found.expiryAt).isEqualTo(expiry)
    }

    @Test
    fun `should find expired uploads`() {
        // given
        val now = Instant.now().truncatedTo(ChronoUnit.MICROS)
        
        val expiredId = StagedEbookUploadId(UUID.randomUUID())
        val expired = StagedEbookUpload(
            id = expiredId,
            fileName = "expired.epub",
            contentType = "application/epub+zip",
            fileSize = 100L,
            metadataJson = null,
            status = StagedEbookUploadStatus.STAGED,
            createdAt = now.minus(48, ChronoUnit.HOURS),
            expiryAt = now.minus(24, ChronoUnit.HOURS)
        )
        
        val notExpiredId = StagedEbookUploadId(UUID.randomUUID())
        val notExpired = StagedEbookUpload(
            id = notExpiredId,
            fileName = "active.epub",
            contentType = "application/epub+zip",
            fileSize = 100L,
            metadataJson = null,
            status = StagedEbookUploadStatus.STAGED,
            createdAt = now.minus(1, ChronoUnit.HOURS),
            expiryAt = now.plus(23, ChronoUnit.HOURS)
        )

        repositoryAdapter.save(expired)
        repositoryAdapter.save(notExpired)

        // when
        val expiredUploads = repositoryAdapter.findByExpiryAtBefore(now)

        // then
        assertThat(expiredUploads).hasSize(1)
        assertThat(expiredUploads.first().id).isEqualTo(expiredId)
    }

    @Test
    fun `should delete upload`() {
        // given
        val id = StagedEbookUploadId(UUID.randomUUID())
        val stagedUpload = StagedEbookUpload(
            id = id,
            fileName = "test.epub",
            contentType = "application/epub+zip",
            fileSize = 1024L,
            metadataJson = null,
            status = StagedEbookUploadStatus.STAGED,
            createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
            expiryAt = Instant.now().truncatedTo(ChronoUnit.MICROS).plus(24, ChronoUnit.HOURS)
        )
        repositoryAdapter.save(stagedUpload)

        // when
        repositoryAdapter.delete(id)

        // then
        assertThat(repositoryAdapter.findById(id)).isNull()
    }
}
