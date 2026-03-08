package com.mikesajak.ebooklib.notification.application

import com.mikesajak.ebooklib.notification.domain.model.NotificationEvent
import com.mikesajak.ebooklib.notification.domain.model.NotificationType
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList

private val logger = KotlinLogging.logger {}

@Service
class NotificationService {
    private val emitters = CopyOnWriteArrayList<SseEmitter>()

    fun createEmitter(): SseEmitter {
        // Create emitter with 30 minute timeout
        val emitter = SseEmitter(30 * 60 * 1000L)
        
        emitter.onCompletion { 
            logger.debug { "SSE emitter completed" }
            emitters.remove(emitter) 
        }
        emitter.onTimeout { 
            logger.debug { "SSE emitter timed out" }
            emitter.complete()
            emitters.remove(emitter) 
        }
        emitter.onError { e -> 
            logger.debug { "SSE emitter error: ${e.message}" }
            emitter.completeWithError(e)
            emitters.remove(emitter) 
        }

        emitters.add(emitter)
        logger.info { "New SSE emitter registered. Total: ${emitters.size}" }
        
        // Send initial connection event
        broadcast(NotificationEvent(NotificationType.SYSTEM_NOTIFICATION, "Connected"))
        
        return emitter
    }

    fun broadcast(event: NotificationEvent) {
        logger.trace { "Broadcasting SSE event: ${event.type}" }
        val deadEmitters = mutableListOf<SseEmitter>()
        
        emitters.forEach { emitter ->
            try {
                emitter.send(
                    SseEmitter.event()
                        .name(event.type.name)
                        .data(event)
                )
            } catch (e: IOException) {
                logger.debug { "Removing dead SSE emitter" }
                deadEmitters.add(emitter)
            }
        }
        
        if (deadEmitters.isNotEmpty()) {
            emitters.removeAll(deadEmitters)
            logger.debug { "Removed ${deadEmitters.size} dead emitters. Remaining: ${emitters.size}" }
        }
    }

    @Scheduled(fixedRate = 30000)
    fun heartbeat() {
        if (emitters.isNotEmpty()) {
            logger.trace { "Sending heartbeat to ${emitters.size} emitters" }
            broadcast(NotificationEvent(NotificationType.SYSTEM_NOTIFICATION, "heartbeat"))
        }
    }
}
