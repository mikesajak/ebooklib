package com.mikesajak.ebooklib.admin.application.ports.incoming

import com.mikesajak.ebooklib.admin.domain.model.SystemSetting

interface SystemSettingsUseCase {
    fun getAllSettings(): List<SystemSetting>
    fun getSetting(key: String): SystemSetting?
    fun updateSetting(key: String, value: String?): SystemSetting
}
