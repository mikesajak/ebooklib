package com.mikesajak.ebooklib.author.infrastructure.adapters.outgoing.persistence

import com.mikesajak.ebooklib.author.application.projection.AuthorProjection
import com.mikesajak.ebooklib.book.infrastructure.adapters.outgoing.persistence.BookEntity
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Root
import jakarta.persistence.criteria.Subquery
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
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
        val content = fetchAuthorProjections(spec, pageable)
        val total = fetchTotalCount(spec)

        return PageImpl(content, pageable, total)
    }

    private fun fetchAuthorProjections(spec: Specification<AuthorEntity>?, pageable: Pageable): List<AuthorProjection> {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(AuthorProjection::class.java)
        val root = query.from(AuthorEntity::class.java)

        val subquery = createBookCountSubquery(query, root)

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

        applySpecification(spec, root, query, cb)

        if (pageable.sort.isSorted) {
            val orders = pageable.sort.map { order ->
                if (order.property == "bookCount") {
                    if (order.isAscending) cb.asc(subquery) else cb.desc(subquery)
                } else {
                    QueryUtils.toOrders(Sort.by(order), root, cb)[0]
                }
            }.toList()
            query.orderBy(orders)
        }

        val typedQuery = entityManager.createQuery(query)
        typedQuery.firstResult = pageable.offset.toInt()
        typedQuery.maxResults = pageable.pageSize

        return typedQuery.resultList
    }

    private fun fetchTotalCount(spec: Specification<AuthorEntity>?): Long {
        val cb = entityManager.criteriaBuilder
        val countQuery = cb.createQuery(Long::class.java)
        val countRoot = countQuery.from(AuthorEntity::class.java)

        countQuery.select(cb.count(countRoot))
        applySpecification(spec, countRoot, countQuery, cb)

        return entityManager.createQuery(countQuery).singleResult
    }

    private fun createBookCountSubquery(query: CriteriaQuery<*>, root: Root<AuthorEntity>): Subquery<Long> {
        val cb = entityManager.criteriaBuilder
        val subquery = query.subquery(Long::class.java)
        val subRoot = subquery.from(BookEntity::class.java)

        subquery.select(cb.count(subRoot))
        subquery.where(cb.isMember(root, subRoot.get("authors")))

        return subquery
    }

    private fun applySpecification(spec: Specification<AuthorEntity>?,
                                   root: Root<AuthorEntity>, query: CriteriaQuery<*>, cb: CriteriaBuilder) {
        if (spec != null) {
            spec.toPredicate(root, query, cb)?.let {
                query.where(it)
            }
        }
    }
}
