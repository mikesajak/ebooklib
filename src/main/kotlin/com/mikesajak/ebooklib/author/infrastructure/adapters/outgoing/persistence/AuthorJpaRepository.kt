package com.mikesajak.ebooklib.author.infrastructure.adapters.outgoing.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface AuthorJpaRepository : JpaRepository<AuthorEntity, UUID>