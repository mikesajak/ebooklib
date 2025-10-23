package com.mikesajak.ebooklib.book.infrastructure.adapters.outgoing.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface BookJpaRepository : JpaRepository<BookEntity, UUID>