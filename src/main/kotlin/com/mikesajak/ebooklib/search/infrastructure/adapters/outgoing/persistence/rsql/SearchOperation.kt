package com.mikesajak.ebooklib.search.infrastructure.adapters.outgoing.persistence.rsql

enum class SearchOperation(val operator: String) {
    EQUAL("=="),
    NOT_EQUAL("!="),
    GREATER_THAN("=gt="),
    GREATER_THAN_OR_EQUAL("=ge="),
    LESS_THAN("=lt="),
    LESS_THAN_OR_EQUAL("=le="),
    IN("=in="),
    NOT_IN("=out="),
    LIKE("=like="),
    NOT_LIKE("=notlike=");

    companion object {
        fun getOperator(operator: String): SearchOperation? {
            return entries.find { it.operator == operator }
        }
    }
}