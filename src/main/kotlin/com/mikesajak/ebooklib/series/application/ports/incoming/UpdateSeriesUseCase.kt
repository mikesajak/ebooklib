package com.mikesajak.ebooklib.series.application.ports.incoming

import com.mikesajak.ebooklib.series.domain.model.Series
import com.mikesajak.ebooklib.series.domain.model.SeriesId

data class UpdateSeriesCommand(
    val id: SeriesId,
    val title: String,
    val description: String?
)

interface UpdateSeriesUseCase {
    fun updateSeries(command: UpdateSeriesCommand): Series
}
