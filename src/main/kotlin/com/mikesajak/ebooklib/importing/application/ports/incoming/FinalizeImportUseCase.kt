package com.mikesajak.ebooklib.importing.application.ports.incoming

import com.mikesajak.ebooklib.author.domain.model.AuthorId
import com.mikesajak.ebooklib.book.domain.model.Book
import com.mikesajak.ebooklib.book.domain.model.BookId
import com.mikesajak.ebooklib.importing.domain.model.StagedEbookUploadId
import com.mikesajak.ebooklib.series.domain.model.SeriesId
import java.time.LocalDate

data class FinalizeImportCommand(
    val uploadId: StagedEbookUploadId,
    val bookId: BookId? = null,
    val title: String,
    val authorIds: List<AuthorId>,
    val authorNames: List<String> = emptyList(), // Names of new authors to be created
    val publisher: String? = null,
    val publicationDate: LocalDate? = null,
    val description: String? = null,
    val seriesId: SeriesId? = null,
    val volume: Int? = null,
    val labels: List<String> = emptyList(),
    val updateCover: Boolean = false,
    val skipFormatLink: Boolean = false // If true, only update metadata, don't link the file
)

interface FinalizeImportUseCase {
    fun finalize(command: FinalizeImportCommand): Book
}
