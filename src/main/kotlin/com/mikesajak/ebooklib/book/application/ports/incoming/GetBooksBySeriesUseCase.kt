package com.mikesajak.ebooklib.book.application.ports.incoming

import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.common.domain.model.PaginatedResult
import com.mikesajak.ebooklib.common.domain.model.PaginationRequest
import com.mikesajak.ebooklib.series.domain.model.SeriesId

interface GetBooksBySeriesUseCase {
    fun getBooksOfSeries(seriesId: SeriesId, pagination: PaginationRequest): PaginatedResult<Book>
}