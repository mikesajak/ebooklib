package com.mikesajak.ebooklib.series.infrastructure.adapters.outgoing.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SeriesJpaRepository : JpaRepository<SeriesEntity, UUID>