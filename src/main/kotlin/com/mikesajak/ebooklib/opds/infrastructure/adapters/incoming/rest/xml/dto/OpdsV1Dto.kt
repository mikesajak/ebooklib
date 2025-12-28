package com.mikesajak.ebooklib.opds.infrastructure.adapters.incoming.rest.xml.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText

@JacksonXmlRootElement(localName = "feed", namespace = "http://www.w3.org/2005/Atom")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AtomFeed(
    @JacksonXmlProperty(localName = "id", namespace = "http://www.w3.org/2005/Atom")
    val id: String,

    @JacksonXmlProperty(localName = "title", namespace = "http://www.w3.org/2005/Atom")
    val title: String,

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

    @JacksonXmlProperty(localName = "dc:language")
    val language: String? = null,
    
    @JacksonXmlProperty(localName = "dc:publisher")
    val publisher: String? = null,
    
    @JacksonXmlProperty(localName = "dc:issued")
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
    val title: String? = null
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
