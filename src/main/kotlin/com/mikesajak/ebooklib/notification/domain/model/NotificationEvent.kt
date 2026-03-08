package com.mikesajak.ebooklib.notification.domain.model

enum class NotificationType {
    IMPORT_PROGRESS,
    STORAGE_SCAN_PROGRESS,
    SYSTEM_NOTIFICATION
}

data class NotificationEvent(
    val type: NotificationType,
    val payload: Any,
    val timestamp: Long = System.currentTimeMillis()
)
