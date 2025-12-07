package com.mikesajak.ebooklib.search.infrastructure.adapters.outgoing.persistence.rsql

import jakarta.persistence.criteria.*
import org.springframework.data.jpa.domain.Specification

class RSQLSpecification<T>(
        private val property: String,
        private val operation: SearchOperation,
        private val values: List<String>,
        private val searchFieldMapper: SearchFieldMapper
) : Specification<T> {

    @Suppress("UNCHECKED_CAST")
    override fun toPredicate(root: Root<T?>,
                             query: CriteriaQuery<*>?,
                             criteriaBuilder: CriteriaBuilder
    ): Predicate? {
        val entityField = searchFieldMapper.mapToEntityField(property)
        val path = getPath(root, entityField)

        return when (operation) {
            SearchOperation.EQUAL ->
                if (values.first() == "null") criteriaBuilder.isNull(path)
                else criteriaBuilder.equal(path, castToRequiredType(path, values))

            SearchOperation.NOT_EQUAL ->
                if (values.first() == "null") criteriaBuilder.isNotNull(path)
                else criteriaBuilder.notEqual(path, castToRequiredType(path, values))

            SearchOperation.GREATER_THAN ->
                criteriaBuilder.greaterThan(path as Path<Comparable<Any>>,
                                            castToRequiredComparableType(path, values))

            SearchOperation.GREATER_THAN_OR_EQUAL ->
                criteriaBuilder.greaterThanOrEqualTo(path as Path<Comparable<Any>>,
                                                     castToRequiredComparableType(path, values))

            SearchOperation.LESS_THAN ->
                criteriaBuilder.lessThan(path as Path<Comparable<Any>>,
                                         castToRequiredComparableType(path, values))

            SearchOperation.LESS_THAN_OR_EQUAL ->
                criteriaBuilder.lessThanOrEqualTo(path as Path<Comparable<Any>>,
                                                  castToRequiredComparableType(path, values))

            SearchOperation.IN ->
                path.`in`(values.map { castToRequiredType(path.javaType, it) })

            SearchOperation.NOT_IN ->
                criteriaBuilder.not(path.`in`(values.map { castToRequiredType(path.javaType, it) }))

            SearchOperation.LIKE ->
                criteriaBuilder.like(criteriaBuilder.lower(path as Path<String>),
                                     "%${values.first().lowercase()}%")

            SearchOperation.NOT_LIKE ->
                criteriaBuilder.notLike(criteriaBuilder.lower(path as Path<String>),
                                        "%${values.first().lowercase()}%")

        }
    }

    private fun castToRequiredType(path: Path<*>, values: List<String>) =
        castToRequiredType(path.javaType, values.first())

    private fun castToRequiredComparableType(path: Path<*>, values: List<String>) =
        castToRequiredType(path, values) as Comparable<Any>

    private fun getPath(root: Root<T?>, entityField: String): Path<*> {
        val parts = property.split(".")

        if (parts.size == 1)
            return root.get<Any>(parts[0])

        var path: Path<*> = root

        // navigate through all parts except the last one
        for (i in 0 until parts.size - 1) {
            path =
                if (path is From<*, *>) getOrCreateJoin(path, parts[i])
                else throw IllegalStateException("Cannot navigate thourgh non-entity property: ${parts[i]}")
        }
        return path.get<Any>(parts.last())
    }

    private fun getOrCreateJoin(from: From<*, *>, attributeName: String): Join<*, *> {
        // check if join already exists to avoid duplicate joins
        val existingJoin = from.joins.find {
            it.attribute.name == attributeName && it.joinType == JoinType.LEFT
        }

        return existingJoin ?: from.join<Any, Any>(attributeName, JoinType.LEFT)
    }

    private fun castToRequiredType(fieldType: Class<*>, value: String): Any {
        return when {
            fieldType.isAssignableFrom(Int::class.java) || fieldType.isAssignableFrom(Integer::class.java) -> value.toInt()
            fieldType.isAssignableFrom(Long::class.java) || fieldType.isAssignableFrom(java.lang.Long::class.java) -> value.toLong()
            fieldType.isAssignableFrom(Double::class.java) || fieldType.isAssignableFrom(java.lang.Double::class.java) -> value.toDouble()
            fieldType.isAssignableFrom(Float::class.java) || fieldType.isAssignableFrom(java.lang.Float::class.java) -> value.toFloat()
            fieldType.isAssignableFrom(Boolean::class.java) || fieldType.isAssignableFrom(java.lang.Boolean::class.java) -> value.toBoolean()
            fieldType.isEnum ->
                fieldType.enumConstants.first { (it as Enum<*>).name.equals(value, ignoreCase = true) }

            else -> value
        }
    }

}