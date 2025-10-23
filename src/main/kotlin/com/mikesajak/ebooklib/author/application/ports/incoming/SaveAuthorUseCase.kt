package com.mikesajak.ebooklib.author.application.ports.incoming

import com.mikesajak.ebooklib.author.domain.model.Author

interface SaveAuthorUseCase {
    fun saveAuthor(author: Author): Author
}