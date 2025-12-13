package com.mikesajak.ebooklib.series.infrastructure.adapters.incoming.rest

import com.mikesajak.ebooklib.book.application.ports.incoming.GetBooksBySeriesUseCase
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.BookRestMapper
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.BookView
import com.mikesajak.ebooklib.book.infrastructure.adapters.incoming.rest.dto.BookResponseDto
import com.mikesajak.ebooklib.infrastructure.incoming.rest.dto.PageResponse
import com.mikesajak.ebooklib.infrastructure.incoming.rest.toPageResponse
import com.mikesajak.ebooklib.infrastructure.web.toDomainPagination
import com.mikesajak.ebooklib.series.application.ports.incoming.AddSeriesUseCase
import com.mikesajak.ebooklib.series.application.ports.incoming.GetSeriesUseCase
import com.mikesajak.ebooklib.series.application.ports.incoming.UpdateSeriesCommand
import com.mikesajak.ebooklib.series.application.ports.incoming.UpdateSeriesUseCase
import com.mikesajak.ebooklib.series.domain.model.SeriesId
import com.mikesajak.ebooklib.series.infrastructure.adapters.incoming.rest.dto.SeriesRequestDto
import com.mikesajak.ebooklib.series.infrastructure.adapters.incoming.rest.dto.SeriesResponseDto
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/series")
class SeriesRestController(
    private val getSeriesUseCase: GetSeriesUseCase,
    private val getBooksBySeriesUseCase: GetBooksBySeriesUseCase,
    private val addSeriesUseCase: AddSeriesUseCase,
    private val updateSeriesUseCase: UpdateSeriesUseCase,
    private val bookRestMapper: BookRestMapper,
    private val seriesRestMapper: SeriesRestMapper
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun addSeries(@Valid @RequestBody seriesRequestDto: SeriesRequestDto): SeriesResponseDto {
        val series = seriesRestMapper.fromRequest(seriesRequestDto)
        val savedSeries = addSeriesUseCase.addSeries(series)
        return seriesRestMapper.toResponse(savedSeries)
    }

    @PutMapping("/{id}")
    fun updateSeries(@PathVariable id: UUID,
                     @Valid @RequestBody seriesRequestDto: SeriesRequestDto): ResponseEntity<SeriesResponseDto> {
        val command = UpdateSeriesCommand(
            id = SeriesId(id),
            title = seriesRequestDto.title,
            description = seriesRequestDto.description
        )
        val updatedSeries = updateSeriesUseCase.updateSeries(command)
        return ResponseEntity.ok(seriesRestMapper.toResponse(updatedSeries))
    }

    @GetMapping
    fun getAllSeries(pageable: Pageable): PageResponse<SeriesResponseDto> =
        getSeriesUseCase.getAllSeries(pageable.toDomainPagination())
            .toPageResponse { series -> seriesRestMapper.toResponse(series) }

    @GetMapping("/{id}")
    fun getSeriesById(@PathVariable id: UUID): SeriesResponseDto =
        seriesRestMapper.toResponse(getSeriesUseCase.getSeries(SeriesId(id)))

    @GetMapping("/{id}/books")
    fun getBooksOfSeries(
        @PathVariable id: UUID, pageable: Pageable,
        @RequestParam(name = "view", required = false, defaultValue = "BY_SERIES") view: BookView
    ): PageResponse<BookResponseDto> {
        return getBooksBySeriesUseCase.getBooksOfSeries(SeriesId(id), pageable.toDomainPagination())
            .toPageResponse { book -> bookRestMapper.toResponse(book, view) }
    }
}