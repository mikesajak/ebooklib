package com.mikesajak.ebooklib.series.domain.exception

import com.mikesajak.ebooklib.series.domain.model.SeriesId

class SeriesNotFoundException(val seriesId: SeriesId) : RuntimeException()