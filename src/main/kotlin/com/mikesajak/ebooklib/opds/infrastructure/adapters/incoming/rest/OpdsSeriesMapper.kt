package com.mikesajak.ebooklib.opds.infrastructure.adapters.incoming.rest

import com.mikesajak.ebooklib.opds.infrastructure.adapters.incoming.rest.dto.Link
import com.mikesajak.ebooklib.series.domain.model.Series
import org.springframework.stereotype.Component

@Component
class OpdsSeriesMapper {
    fun toNavigationLink(series: Series): Link {
        val seriesBooksHref = "/opds/v2/series/${series.id!!.value}/books.json"
        return Link(
            href = seriesBooksHref,
            type = OpdsV2Controller.OPDS_JSON_MEDIA_TYPE,
            rel = "subsection",
            title = series.title
        )
    }
}
