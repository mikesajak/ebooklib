package com.mikesajak.ebooklib.infrastructure.security.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserJpaRepository : JpaRepository<UserEntity, UUID> {
    fun findByUsername(username: String): UserEntity?
}
