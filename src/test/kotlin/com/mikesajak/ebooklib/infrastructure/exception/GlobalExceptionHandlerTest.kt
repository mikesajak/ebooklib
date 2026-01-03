package com.mikesajak.ebooklib.infrastructure.exception

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.context.request.async.AsyncRequestNotUsableException
import java.io.IOException

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    fun `handleGeneralException should handle AsyncRequestNotUsableException as client disconnect`() {
        val exception = AsyncRequestNotUsableException("Client gone")
        
        val response = handler.handleGeneralException(exception)
        
        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(response.body).isNull()
    }

    @Test
    fun `handleGeneralException should handle IOException with Broken pipe as client disconnect`() {
        val exception = Exception(IOException("Broken pipe"))
        
        val response = handler.handleGeneralException(exception)
        
        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(response.body).isNull()
    }

    @Test
    fun `handleGeneralException should handle generic exception normally`() {
        val exception = RuntimeException("Some other error")
        
        val response = handler.handleGeneralException(exception)
        
        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(response.body).isNotNull
        assertThat(response.body?.message).isEqualTo("An unexpected error occurred")
        assertThat(response.headers.contentType?.toString()).isEqualTo("application/json")
    }

    @Test
    fun `handleAsyncRequestNotUsableException should handle exception explicitly`() {
        val exception = AsyncRequestNotUsableException("Client gone")
        
        // This method returns Unit, just check it doesn't crash
        handler.handleAsyncRequestNotUsableException(exception)
    }
}
