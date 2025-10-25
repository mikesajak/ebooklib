package com.mikesajak.ebooklib.book.application.ports.incoming

import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.series.domain.model.SeriesId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface GetBooksBySeriesUseCase {
    fun getBooksOfSeries(seriesId: SeriesId, pageable: Pageable): Page<Book>
}