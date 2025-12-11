package com.mikesajak.ebooklib.series.application.ports.incoming

import com.mikesajak.ebooklib.series.domain.model.Series

interface AddSeriesUseCase {
    fun addSeries(series: Series): Series
}
