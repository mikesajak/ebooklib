package com.mikesajak.ebooklib.infrastructure.exception

import com.mikesajak.ebooklib.author.domain.exception.AuthorNotFoundException
import com.mikesajak.ebooklib.book.domain.exception.BookNotFoundException
import com.mikesajak.ebooklib.book.domain.exception.EbookFormatFileNotFoundException
import com.mikesajak.ebooklib.search.infrastructure.adapters.outgoing.persistence.rsql.SearchQueryException
import com.mikesajak.ebooklib.series.domain.exception.SeriesNotFoundException
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.context.request.async.AsyncRequestNotUsableException
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.io.IOException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = KotlinLogging.logger {}

    @ExceptionHandler(NoHandlerFoundException::class)
    fun handleNoHandlerFoundException(e: NoHandlerFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .contentType(MediaType.APPLICATION_JSON)
            .body(ErrorResponse(HttpStatus.NOT_FOUND.value(), e.message ?: "Resource not found"))
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException(e: NoResourceFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .contentType(MediaType.APPLICATION_JSON)
            .body(ErrorResponse(HttpStatus.NOT_FOUND.value(), e.message ?: "Resource not found"))
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException::class)
    fun handleHttpMediaTypeNotAcceptableException(e: HttpMediaTypeNotAcceptableException): ResponseEntity<Void> {
        return ResponseEntity
            .status(HttpStatus.NOT_ACCEPTABLE)
            .build()
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleHttpMediaTypeNotSupportedException(e: HttpMediaTypeNotSupportedException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
            .contentType(MediaType.APPLICATION_JSON)
            .body(ErrorResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), e.message ?: "Media type not supported"))
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleHttpRequestMethodNotSupportedException(e: HttpRequestMethodNotSupportedException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.METHOD_NOT_ALLOWED)
            .contentType(MediaType.APPLICATION_JSON)
            .body(ErrorResponse(HttpStatus.METHOD_NOT_ALLOWED.value(), e.message ?: "Method not allowed"))
    }

    @ExceptionHandler(BookNotFoundException::class)
    fun handleBookNotFound(e: BookNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponse(HttpStatus.NOT_FOUND.value(), e.message ?: "Book id=${e.bookId} not found"))
    }

    @ExceptionHandler(AuthorNotFoundException::class)
    fun handleAuthorNotFound(e: AuthorNotFoundException): ResponseEntity<ErrorResponse> {
        logger.info("Author not found: ${e.message}")
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponse(HttpStatus.NOT_FOUND.value(), e.message ?: "Author id=${e.authorId} not found"))
    }

    @ExceptionHandler(SeriesNotFoundException::class)
    fun handleSeriesNotFound(e: SeriesNotFoundException): ResponseEntity<ErrorResponse?> =
        ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponse(HttpStatus.NOT_FOUND.value(), e.message ?: "Series id=${e.seriesId} not found"))

    @ExceptionHandler(EbookFormatFileNotFoundException::class)
    fun handleEbookFormatFileNotFound(e: EbookFormatFileNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponse(HttpStatus.NOT_FOUND.value(), e.message ?: "Ebook format file not found"))
    }

    @ExceptionHandler(SearchQueryException::class)
    fun handleSearchQueryException(e: SearchQueryException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponse(HttpStatus.BAD_REQUEST.value(), e.message ?: "Invalid search query"))
    }

    @ExceptionHandler(AsyncRequestNotUsableException::class)
    fun handleAsyncRequestNotUsableException(e: AsyncRequestNotUsableException) {
        logger.debug { "Async request not usable (likely client disconnected): ${e.message}" }
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneralException(e: Exception): ResponseEntity<ErrorResponse> {
        if (isClientAbortException(e)) {
            logger.debug { "Client disconnected: ${e.message}" }
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build()
        }

        logger.error("Unexpected error occurred", e)
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected error occurred"))
    }

    private fun isClientAbortException(e: Throwable): Boolean {
        if (e is AsyncRequestNotUsableException) return true
        var cause: Throwable? = e
        while (cause != null) {
            if (cause is IOException && cause.message?.contains("Broken pipe", ignoreCase = true) == true) return true
            if (cause.javaClass.simpleName == "ClientAbortException") return true
            cause = cause.cause
        }
        return false
    }
}

data class ErrorResponse(val status: Int, val message: String)