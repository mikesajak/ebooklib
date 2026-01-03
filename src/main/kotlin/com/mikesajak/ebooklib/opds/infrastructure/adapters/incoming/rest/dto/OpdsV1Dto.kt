package com.mikesajak.ebooklib.opds.infrastructure.adapters.incoming.rest.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText

@JacksonXmlRootElement(localName = "feed", namespace = "http://www.w3.org/2005/Atom")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AtomFeed(
    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:dc")
    val dcNamespace: String = "http://purl.org/dc/terms/",

    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:opds")
    val opdsNamespace: String = "http://opds-spec.org/2010/catalog",

    @JacksonXmlProperty(localName = "id", namespace = "http://www.w3.org/2005/Atom")
    val id: String,

    @JacksonXmlProperty(localName = "title", namespace = "http://www.w3.org/2005/Atom")
    val title: String,

    @JacksonXmlProperty(localName = "subtitle", namespace = "http://www.w3.org/2005/Atom")
    val subtitle: String? = null,

    @JacksonXmlProperty(localName = "icon", namespace = "http://www.w3.org/2005/Atom")
    val icon: String? = null,

    @JacksonXmlProperty(localName = "updated", namespace = "http://www.w3.org/2005/Atom")
    val updated: String,

    @JacksonXmlProperty(localName = "author", namespace = "http://www.w3.org/2005/Atom")
    val author: AtomAuthor? = null,

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "link", namespace = "http://www.w3.org/2005/Atom")
    val links: List<AtomLink> = emptyList(),

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "entry", namespace = "http://www.w3.org/2005/Atom")
    val entries: List<AtomEntry> = emptyList()
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AtomEntry(
    @JacksonXmlProperty(localName = "id", namespace = "http://www.w3.org/2005/Atom")
    val id: String,

    @JacksonXmlProperty(localName = "title", namespace = "http://www.w3.org/2005/Atom")
    val title: String,

    @JacksonXmlProperty(localName = "updated", namespace = "http://www.w3.org/2005/Atom")
    val updated: String,

    @JacksonXmlProperty(localName = "author", namespace = "http://www.w3.org/2005/Atom")
    val author: List<AtomAuthor>? = null,

    @JacksonXmlProperty(localName = "summary", namespace = "http://www.w3.org/2005/Atom")
    val summary: String? = null,

    @JacksonXmlProperty(localName = "content", namespace = "http://www.w3.org/2005/Atom")
    val content: AtomContent? = null,

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "link", namespace = "http://www.w3.org/2005/Atom")
    val links: List<AtomLink> = emptyList(),

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "category", namespace = "http://www.w3.org/2005/Atom")
    val categories: List<AtomCategory> = emptyList(),
    
    @JacksonXmlProperty(localName = "published", namespace = "http://www.w3.org/2005/Atom")
    val published: String? = null,

    @JacksonXmlProperty(localName = "language", namespace = "http://purl.org/dc/terms/")
    val language: String? = null,
    
    @JacksonXmlProperty(localName = "publisher", namespace = "http://purl.org/dc/terms/")
    val publisher: String? = null,
    
    @JacksonXmlProperty(localName = "issued", namespace = "http://purl.org/dc/terms/")
    val issued: String? = null
)

data class AtomAuthor(
    @JacksonXmlProperty(localName = "name", namespace = "http://www.w3.org/2005/Atom")
    val name: String,
    
    @JacksonXmlProperty(localName = "uri", namespace = "http://www.w3.org/2005/Atom")
    val uri: String? = null
)

data class AtomLink(
    @JacksonXmlProperty(isAttribute = true)
    val href: String,

    @JacksonXmlProperty(isAttribute = true)
    val rel: String? = null,

    @JacksonXmlProperty(isAttribute = true)
    val type: String? = null,
    
    @JacksonXmlProperty(isAttribute = true)
    val title: String? = null,

    @JacksonXmlProperty(isAttribute = true)
    val length: Long? = null,

    @JacksonXmlProperty(isAttribute = true)
    val mtime: String? = null
)

data class AtomContent(
    @JacksonXmlProperty(isAttribute = true)
    val type: String,
    
    @JacksonXmlText
    val text: String
)

data class AtomCategory(
    @JacksonXmlProperty(isAttribute = true)
    val term: String,
    
    @JacksonXmlProperty(isAttribute = true)
    val scheme: String? = null,
    
    @JacksonXmlProperty(isAttribute = true)
    val label: String? = null
)

@JacksonXmlRootElement(localName = "OpenSearchDescription", namespace = "http://a9.com/-/spec/opensearch/1.1/")
data class OpenSearchDescription(
    @JacksonXmlProperty(localName = "ShortName", namespace = "http://a9.com/-/spec/opensearch/1.1/")
    val shortName: String,

    @JacksonXmlProperty(localName = "Description", namespace = "http://a9.com/-/spec/opensearch/1.1/")
    val description: String,

    @JacksonXmlProperty(localName = "InputEncoding", namespace = "http://a9.com/-/spec/opensearch/1.1/")
    val inputEncoding: String = "UTF-8",

    @JacksonXmlProperty(localName = "OutputEncoding", namespace = "http://a9.com/-/spec/opensearch/1.1/")
    val outputEncoding: String = "UTF-8",

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Url", namespace = "http://a9.com/-/spec/opensearch/1.1/")
    val urls: List<OpenSearchUrl>
)

data class OpenSearchUrl(
    @JacksonXmlProperty(isAttribute = true)
    val type: String,

    @JacksonXmlProperty(isAttribute = true)
    val template: String
)
