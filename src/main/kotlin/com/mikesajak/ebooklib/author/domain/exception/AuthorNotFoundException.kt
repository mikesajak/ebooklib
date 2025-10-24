package com.mikesajak.ebooklib.author.domain.exception

import com.mikesajak.ebooklib.author.domain.model.AuthorId

class AuthorNotFoundException(val authorId: AuthorId) : RuntimeException()