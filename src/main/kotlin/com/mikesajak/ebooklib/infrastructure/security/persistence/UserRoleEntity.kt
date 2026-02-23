package com.mikesajak.ebooklib.infrastructure.security.persistence

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "user_roles")
class UserRoleEntity(
    @Id
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,

    @Column(nullable = false)
    val role: String
)
