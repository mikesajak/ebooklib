package com.mikesajak.ebooklib.author.application.ports.incoming

import com.mikesajak.ebooklib.author.domain.model.Author
import com.mikesajak.ebooklib.author.domain.model.AuthorId
import java.time.LocalDate

data class UpdateAuthorCommand(
    val id: AuthorId,
    val firstName: String,
    val lastName: String,
    val bio: String?,
    val birthDate: LocalDate?,
    val deathDate: LocalDate?
)

interface UpdateAuthorUseCase {
    fun updateAuthor(command: UpdateAuthorCommand): Author
}
