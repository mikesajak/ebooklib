package com.mikesajak.ebooklib.search.infrastructure.adapters.outgoing.persistence.rsql

import org.springframework.stereotype.Component

sealed interface FieldMapping {
    data class Simple(val path: String) : FieldMapping
    data class Composite(val paths: List<String>, val separator: String = " ") : FieldMapping
}

@Component
class SearchFieldMapper {
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

    fun getMapping(domainField: String): FieldMapping {
        if (!allowedFields.contains(domainField))
            throw IllegalArgumentException("Field $domainField is not allowed for search")

        return fieldMappings[domainField]
            ?: throw IllegalArgumentException("Field $domainField is not allowed for search")
    }

    // Deprecated: use getMapping instead. Kept for temporary compatibility if needed, 
    // but we are updating RSQLSpecification immediately.
    // However, mapToEntityField returned String. Simple mapping can return string.
    // Composite mapping cannot be represented as simple string.
    // So I will remove mapToEntityField to force compilation error if used.

    fun isFieldAllowed(field: String): Boolean = allowedFields.contains(field)

    fun getAllowedFields(): Set<String> = allowedFields
}
