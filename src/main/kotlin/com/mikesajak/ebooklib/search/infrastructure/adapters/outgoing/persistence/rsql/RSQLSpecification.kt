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
        val mapping = searchFieldMapper.getMapping(property)
        val expression = resolveExpression(root, mapping, criteriaBuilder)

        return when (operation) {
            SearchOperation.EQUAL ->
                if (values.first() == "null") criteriaBuilder.isNull(expression)
                else criteriaBuilder.equal(expression, castToRequiredType(expression, values))

            SearchOperation.NOT_EQUAL ->
                if (values.first() == "null") criteriaBuilder.isNotNull(expression)
                else criteriaBuilder.notEqual(expression, castToRequiredType(expression, values))

            SearchOperation.GREATER_THAN ->
                criteriaBuilder.greaterThan(expression as Expression<Comparable<Any>>,
                                            castToRequiredComparableType(expression, values))

            SearchOperation.GREATER_THAN_OR_EQUAL ->
                criteriaBuilder.greaterThanOrEqualTo(expression as Expression<Comparable<Any>>,
                                                     castToRequiredComparableType(expression, values))

            SearchOperation.LESS_THAN ->
                criteriaBuilder.lessThan(expression as Expression<Comparable<Any>>,
                                         castToRequiredComparableType(expression, values))

            SearchOperation.LESS_THAN_OR_EQUAL ->
                criteriaBuilder.lessThanOrEqualTo(expression as Expression<Comparable<Any>>,
                                                  castToRequiredComparableType(expression, values))

            SearchOperation.IN ->
                expression.`in`(values.map { castToRequiredType(expression.javaType, it) })

            SearchOperation.NOT_IN ->
                criteriaBuilder.not(expression.`in`(values.map { castToRequiredType(expression.javaType, it) }))

            SearchOperation.LIKE ->
                criteriaBuilder.like(criteriaBuilder.lower(expression as Expression<String>),
                                     "%${values.first().lowercase()}%")

            SearchOperation.NOT_LIKE ->
                criteriaBuilder.notLike(criteriaBuilder.lower(expression as Expression<String>),
                                        "%${values.first().lowercase()}%")

        }
    }

    private fun resolveExpression(root: Root<T?>, mapping: FieldMapping, cb: CriteriaBuilder): Expression<*> {
        return when (mapping) {
            is FieldMapping.Simple -> getPath(root, mapping.path)
            is FieldMapping.Composite -> {
                if (mapping.paths.isEmpty()) throw IllegalStateException("Composite mapping must have at least one path")
                
                mapping.paths.map { getPath(root, it) as Expression<String> }.reduce { acc, expr ->
                    cb.concat(cb.concat(acc, mapping.separator), expr)
                }
            }
        }
    }

    private fun castToRequiredType(expression: Expression<*>, values: List<String>) =
        castToRequiredType(expression.javaType, values.first())

    private fun castToRequiredComparableType(expression: Expression<*>, values: List<String>) =
        castToRequiredType(expression, values) as Comparable<Any>

    private fun getPath(root: Root<T?>, entityFieldPath: String): Path<*> {
        val parts = entityFieldPath.split(".")

        if (parts.size == 1)
            return root.get<Any>(parts[0])

        var path: Path<*> = root

        // navigate through all parts except the last one
        for (i in 0 until parts.size - 1) {
            path =
                if (path is From<*, *>) getOrCreateJoin(path, parts[i])
                else throw IllegalStateException("Cannot navigate through non-entity property: ${parts[i]}")
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
