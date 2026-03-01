package com.mikesajak.ebooklib.admin.application.services

import com.mikesajak.ebooklib.admin.infrastructure.adapters.outgoing.persistence.SystemSettingsEntity
import com.mikesajak.ebooklib.admin.infrastructure.adapters.outgoing.persistence.SystemSettingsJpaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

class SystemSettingsServiceTest {

    private val repository = mockk<SystemSettingsJpaRepository>()
    private val service = SystemSettingsService(repository)

    @Test
    fun `should return all settings`() {
        // given
        val entities = listOf(
            SystemSettingsEntity("key1", "value1", "desc1"),
            SystemSettingsEntity("key2", "value2", "desc2")
        )
        every { repository.findAll() } returns entities

        // when
        val result = service.getAllSettings()

        // then
        assertThat(result).hasSize(2)
        assertThat(result[0].key).isEqualTo("key1")
        assertThat(result[1].key).isEqualTo("key2")
    }

    @Test
    fun `should update existing setting`() {
        // given
        val key = "some.key"
        val existingEntity = SystemSettingsEntity(key, "old.value", "desc")
        every { repository.findById(key) } returns Optional.of(existingEntity)
        every { repository.save(any()) } answers { it.invocation.args[0] as SystemSettingsEntity }

        // when
        val result = service.updateSetting(key, "new.value")

        // then
        assertThat(result.value).isEqualTo("new.value")
        verify { repository.save(match { it.key == key && it.value == "new.value" }) }
    }

    @Test
    fun `should create new setting if not exists during update`() {
        // given
        val key = "new.key"
        every { repository.findById(key) } returns Optional.empty()
        every { repository.save(any()) } answers { it.invocation.args[0] as SystemSettingsEntity }

        // when
        val result = service.updateSetting(key, "some.value")

        // then
        assertThat(result.key).isEqualTo(key)
        assertThat(result.value).isEqualTo("some.value")
        verify { repository.save(match { it.key == key && it.value == "some.value" }) }
    }
}
