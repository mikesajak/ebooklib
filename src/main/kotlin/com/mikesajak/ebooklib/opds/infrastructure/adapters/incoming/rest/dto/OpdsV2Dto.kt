package com.mikesajak.ebooklib.opds.infrastructure.adapters.incoming.rest.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

// Based on OPDS 2.0 specification

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Feed(
        val metadata: OpdsMetadata,
        val links: List<Link>,
        val publications: List<Publication>? = null,
        val navigation: List<Link>? = null,
        val groups: List<Group>? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OpdsMetadata(
        val title: String,
        @JsonProperty("number_of_items")
        val numberOfItems: Int? = null,
        @JsonProperty("items_per_page")
        val itemsPerPage: Int? = null,
        @JsonProperty("current_page")
        val currentPage: Int? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Link(
        val href: String,
        val type: String? = null,
        val rel: String? = null, // Can also be a list of strings
        val title: String? = null,
        val templated: Boolean? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Publication(
        val metadata: PublicationMetadata,
        val links: List<Link>,
        val images: List<Link>? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PublicationMetadata(
        val title: String,
        val author: List<Contributor>? = null,
        val identifier: String? = null,
        val published: String? = null,
        val modified: String? = null,
        val language: String? = null,
        val description: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Contributor(
        val name: String,
        val identifier: String? = null,
        @JsonProperty("sort_as")
        val sortAs: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Group(
        val metadata: OpdsMetadata,
        val links: List<Link>? = null,
        val publications: List<Publication>? = null,
        val navigation: List<Link>? = null
)
