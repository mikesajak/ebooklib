package com.mikesajak.ebooklib.admin.infrastructure.adapters.outgoing.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SystemSettingsJpaRepository : JpaRepository<SystemSettingsEntity, String>
