package com.mikesajak.ebooklib.search.infrastructure.adapters.outgoing.persistence.rsql

import cz.jirutka.rsql.parser.RSQLParser
import cz.jirutka.rsql.parser.ast.ComparisonOperator
import mu.KotlinLogging
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Component

@Component
class RSQLSpecParser {
    private val logger = KotlinLogging.logger {}
    private val parser = initParser()

    fun <T> parse(query: String, searchFieldMapper: SearchFieldMapper): Specification<T> {
        val rootNode = parser.parse(query)
        return rootNode.accept(RSQLVisitorSpecBuilder(searchFieldMapper))
    }

    fun <T> parseOrNull(query: String?, searchFieldMapper: SearchFieldMapper): Specification<T>? {
        if (query.isNullOrBlank()) return null

        return try {
            parse(query, searchFieldMapper)
        } catch (e: Exception) {
            logger.warn("Failed to parse search query: $query", e)
            throw SearchQueryException("Failed to parse search query: ${e.message}, query: $query")
        }
    }

    private fun initParser(): RSQLParser {
        return RSQLParser(SearchOperation.entries.map { it.operator }
                                  .map { ComparisonOperator(it) }
                                  .toSet())
    }
}

class SearchQueryException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)