package com.basalt.ai.controller;

import com.basalt.ai.model.ChatRequest;
import com.basalt.ai.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * REST controller for all chat interactions.
 *
 * <p><b>Endpoint:</b> {@code POST /api/chat/stream}
 *
 * <p>Returns a Server-Sent Events (SSE) stream of text tokens produced by the
 * local Ollama LLM via Spring AI. The Angular frontend consumes this stream
 * using the native {@code EventSource} API, rendering tokens incrementally
 * for a Gemini-like typing effect.
 *
 * <h3>Request body (JSON):</h3>
 * <pre>{@code
 * {
 *   "message": "Explain the SOLID principles with Java examples.",
 *   "conversationId": "optional-uuid",
 *   "useRag": false
 * }
 * }</pre>
 *
 * <h3>Response (text/event-stream):</h3>
 * <pre>{@code
 * data: The SOLID principles are...
 * data:  a set of five design guidelines...
 * data: [DONE]
 * }</pre>
 */
@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * Streams an LLM response as Server-Sent Events.
     *
     * <p>Spring WebFlux serialises the {@link Flux} over the HTTP response
     * as {@code text/event-stream}. Each emission is a raw text token.
     * A terminal {@code [DONE]} event signals end-of-stream to the client.
     *
     * @param request validated chat request body
     * @return reactive token stream
     */
    @PostMapping(
            value = "/stream",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<String> streamChat(@Valid @RequestBody ChatRequest request) {
        log.info("Chat stream request — conversationId={}, useRag={}",
                request.getConversationId(), request.isUseRag());

        return chatService
                .streamChat(request.getMessage(), request.isUseRag())
                .concatWith(Flux.just("[DONE]"))          // terminal sentinel
                .doOnError(e -> log.error("Stream error: {}", e.getMessage(), e));
    }
}

