package com.mikesajak.ebooklib.series.infrastructure.adapters.outgoing.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import java.util.*

interface SeriesJpaRepository : JpaRepository<SeriesEntity, UUID>, JpaSpecificationExecutor<SeriesEntity>