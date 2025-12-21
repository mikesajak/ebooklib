package com.mikesajak.ebooklib.search.infrastructure.adapters.outgoing.persistence.rsql

import org.springframework.stereotype.Component

sealed interface FieldMapping {
    data class Simple(val path: String) : FieldMapping
    data class Composite(val paths: List<String>, val separator: String = " ") : FieldMapping
}

interface SearchFieldMapper {
    fun getMapping(domainField: String): FieldMapping
    fun isFieldAllowed(field: String): Boolean
    fun getAllowedFields(): Set<String>
}

@Component
class BookSearchFieldMapper : SearchFieldMapper {
    private val fieldMappings: Map<String, FieldMapping> = mapOf(
            "title" to FieldMapping.Simple("title"),
            "description" to FieldMapping.Simple("description"),

            // nested mappings
            "authors.firstName" to FieldMapping.Simple("authors.firstName"),
            "authors.lastName" to FieldMapping.Simple("authors.lastName"),
            "author.description" to FieldMapping.Simple("authors.description"),
            "author.bio" to FieldMapping.Simple("authors.description"),

            "author.name" to FieldMapping.Composite(listOf("authors.firstName", "authors.lastName"), " "),
            "authors.name" to FieldMapping.Composite(listOf("authors.firstName", "authors.lastName"), " "),

            "series.name" to FieldMapping.Simple("series.name"),
            "series.volume" to FieldMapping.Simple("series.volume"),
    )

    private val allowedFields = fieldMappings.keys

    override fun getMapping(domainField: String): FieldMapping {
        if (!allowedFields.contains(domainField))
            throw IllegalArgumentException("Field $domainField is not allowed for search")

        return fieldMappings[domainField]
            ?: throw IllegalArgumentException("Field $domainField is not allowed for search")
    }

    override fun isFieldAllowed(field: String): Boolean = allowedFields.contains(field)

    override fun getAllowedFields(): Set<String> = allowedFields
}
