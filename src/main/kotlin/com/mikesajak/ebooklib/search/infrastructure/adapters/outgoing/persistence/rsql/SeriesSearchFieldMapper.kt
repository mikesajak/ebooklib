package com.mikesajak.ebooklib.search.infrastructure.adapters.outgoing.persistence.rsql

import org.springframework.stereotype.Component

@Component
class SeriesSearchFieldMapper : SearchFieldMapper {
    private val fieldMappings: Map<String, FieldMapping> = mapOf(
            "title" to FieldMapping.Simple("title"),
            "description" to FieldMapping.Simple("description")
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
