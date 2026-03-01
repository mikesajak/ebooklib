package com.mikesajak.ebooklib.admin.infrastructure.adapters.outgoing.persistence

import jakarta.persistence.*

@Entity
@Table(name = "system_settings")
class SystemSettingsEntity(
    @Id
    @Column(name = "key")
    var key: String,

    @Column(name = "value")
    var value: String? = null,

    @Column(name = "description")
    var description: String? = null
)
