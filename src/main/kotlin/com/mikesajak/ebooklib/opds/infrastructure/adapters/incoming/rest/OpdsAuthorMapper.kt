package com.mikesajak.ebooklib.opds.infrastructure.adapters.incoming.rest

import com.mikesajak.ebooklib.author.domain.model.Author
import com.mikesajak.ebooklib.opds.infrastructure.adapters.incoming.rest.dto.Link
import org.springframework.stereotype.Component

@Component
class OpdsAuthorMapper {
    fun toNavigationLink(author: Author): Link {
        val authorBooksHref = "/opds/v2/authors/${author.id!!.value}/books.json"
        return Link(
                href = authorBooksHref,
                type = OpdsController.OPDS_JSON_MEDIA_TYPE,
                rel = "subsection",
                title = "${author.firstName} ${author.lastName}"
        )
    }
}
