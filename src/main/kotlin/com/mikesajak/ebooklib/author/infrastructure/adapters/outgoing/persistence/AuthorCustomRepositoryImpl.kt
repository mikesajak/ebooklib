package com.mikesajak.ebooklib.author.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.author.application.projection.AuthorProjection
import com.mikesajak.ebooklib.book.infrastructure.adapters.outgoing.persistence.BookEntity
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.JoinType
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.query.QueryUtils
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class AuthorCustomRepositoryImpl(
    @PersistenceContext
    private val entityManager: EntityManager
) : AuthorCustomRepository {

    override fun findAuthorsWithBookCount(spec: Specification<AuthorEntity>?, pageable: Pageable): Page<AuthorProjection> {
        val cb = entityManager.criteriaBuilder

        // 1. Fetch content
        val query = cb.createQuery(AuthorProjection::class.java)
        val root = query.from(AuthorEntity::class.java)

        // Subquery for book count
        val subquery = query.subquery(Long::class.java)
        val subRoot = subquery.from(BookEntity::class.java)
        subquery.select(cb.count(subRoot))
        subquery.where(cb.isMember(root, subRoot.get<Collection<AuthorEntity>>("authors")))

        // Projection
        query.select(cb.construct(
            AuthorProjection::class.java,
            root.get<UUID>("id"),
            root.get<String>("firstName"),
            root.get<String>("lastName"),
            root.get<String>("bio"),
            root.get<java.time.LocalDate>("birthDate"),
            root.get<java.time.LocalDate>("deathDate"),
            subquery
        ))

        // Predicates from Specification
        if (spec != null) {
            val predicate = spec.toPredicate(root, query, cb)
            if (predicate != null) {
                query.where(predicate)
            }
        }

        // Sorting
        if (pageable.sort.isSorted) {
            // Note: QueryUtils.toOrders is a Spring helper to convert Sort to Criteria Orders
            query.orderBy(QueryUtils.toOrders(pageable.sort, root, cb))
        }

        val typedQuery = entityManager.createQuery(query)
        typedQuery.firstResult = pageable.offset.toInt()
        typedQuery.maxResults = pageable.pageSize

        val content = typedQuery.resultList

        // 2. Fetch total count
        val countQuery = cb.createQuery(Long::class.java)
        val countRoot = countQuery.from(AuthorEntity::class.java)
        countQuery.select(cb.count(countRoot))
        if (spec != null) {
            val predicate = spec.toPredicate(countRoot, countQuery, cb)
            if (predicate != null) {
                countQuery.where(predicate)
            }
        }
        val total = entityManager.createQuery(countQuery).singleResult

        return PageImpl(content, pageable, total)
    }
}
