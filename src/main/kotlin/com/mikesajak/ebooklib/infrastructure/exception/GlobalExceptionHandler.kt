package com.mikesajak.ebooklib.infrastructure.exception
import mu.KotlinLogging

import com.mikesajak.ebooklib.author.domain.exception.AuthorNotFoundException
import com.mikesajak.ebooklib.book.domain.exception.BookNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
private val logger = KotlinLogging.logger {}

    @ExceptionHandler(BookNotFoundException::class)
    fun handleBookNotFound(e: BookNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(HttpStatus.NOT_FOUND.value(), e.message ?: "Book id=${e.bookId} not found"))
    }

    @ExceptionHandler(AuthorNotFoundException::class)
    fun handleAuthorNotFound(e: AuthorNotFoundException): ResponseEntity<ErrorResponse> {
        logger.info("Author not found: ${e.message}")
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(HttpStatus.NOT_FOUND.value(), e.message ?: "Author id=${e.authorId } not found"))
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneralException(e: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error occurred", e)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected error occurred"))
    }
}

data class ErrorResponse(val status: Int, val message: String)