package com.mikesajak.ebooklib.search.infrastructure.adapters.outgoing.persistence.rsql

import cz.jirutka.rsql.parser.ast.AndNode
import cz.jirutka.rsql.parser.ast.ComparisonNode
import cz.jirutka.rsql.parser.ast.OrNode
import cz.jirutka.rsql.parser.ast.RSQLVisitor
import org.springframework.data.jpa.domain.Specification

class RSQLVisitorSpecBuilder<T>(
        private val searchFieldMapper: SearchFieldMapper
) : RSQLVisitor<Specification<T>, Void?> {

    override fun visit(node: AndNode, param: Void?): Specification<T> {
        return node.children
                .map { it.accept(this, param) }
                .reduce { acc, spec -> acc.and(spec) }
    }

    override fun visit(node: OrNode, param: Void?): Specification<T> {
        return node.children
                .map { it.accept(this, param) }
                .reduce { acc, spec -> acc.or(spec) }
    }

    override fun visit(node: ComparisonNode, param: Void?): Specification<T> {
        try {
            searchFieldMapper.getMapping(node.selector)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Field '${node.selector}' is not allowed for search", e)
        }

        val operation = SearchOperation.getOperator(node.operator.symbol)
            ?: throw IllegalArgumentException("Unsupported operator: ${node.operator}")

        return RSQLSpecification(
                property = node.selector,
                operation = operation,
                values = node.arguments,
                searchFieldMapper = searchFieldMapper)
    }

}