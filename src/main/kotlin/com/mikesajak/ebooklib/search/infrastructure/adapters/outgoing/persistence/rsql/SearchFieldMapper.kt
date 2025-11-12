package com.mikesajak.ebooklib.search.infrastructure.adapters.outgoing.persistence.rsql

import org.springframework.stereotype.Component

@Component
class SearchFieldMapper {
    private val fieldMappings = mapOf(
            "title" to "title",
            "description" to "description",

            // nested mappings
            "authors.firstName" to "authors.firstName",
            "authors.lastName" to "authors.lastName",
            "author.description" to "authors.description",
            "author.bio" to "authors.description",

            "series.name" to "series.name",
            "series.volume" to "series.volume",
    )

    private val allowedFields = fieldMappings.keys

    fun mapToEntityField(domainField: String): String {
        if (!allowedFields.contains(domainField))
            throw IllegalArgumentException("Field $domainField is not allowed for search") // TODO: use domain exception

        return fieldMappings[domainField]
            ?: throw IllegalArgumentException("Field $domainField is not allowed for search")
    }

    fun isFieldAllowed(field: String): Boolean = allowedFields.contains(field)

    fun getAllowedFields(): Set<String> = allowedFields
}