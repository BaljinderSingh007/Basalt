package com.basalt.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Core chat service.
 *
 * <p>Orchestrates the full chat pipeline:
 * <ol>
 *   <li>Optionally enriches the prompt with RAG context from PgVector.</li>
 *   <li>Streams tokens from the local Ollama LLM via Spring AI's reactive API.</li>
 * </ol>
 *
 * <p>The returned {@link Flux} emits individual text tokens as they arrive,
 * enabling true server-sent-event streaming to the Angular frontend.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;
    private final RagService ragService;

    /**
     * Streams a response for the given user message.
     *
     * @param userMessage the raw user input
     * @param useRag      whether to prepend RAG context to the prompt
     * @return a {@link Flux} of token strings
     */
    public Flux<String> streamChat(String userMessage, boolean useRag) {
        String augmentedPrompt = buildPrompt(userMessage, useRag);
        log.debug("Streaming chat — useRag={}, promptLength={}", useRag, augmentedPrompt.length());

        return chatClient.prompt()
                .user(augmentedPrompt)
                .stream()
                .content();
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private String buildPrompt(String userMessage, boolean useRag) {
        // Always include creator context
        String systemContext = "You are Basalt AI Assistant, created by Baljinder Singh. " +
                "When asked about your creator or who made you, respond that you were created by Baljinder Singh. " +
                "If asked for contact information, provide this LinkedIn profile: https://www.linkedin.com/in/baljinder-singh-013b4311b/\n\n";
        
        if (!useRag) {
            return systemContext + userMessage;
        }
        String context = ragService.retrieveContext(userMessage);
        return systemContext + (context.isBlank() ? userMessage : context + userMessage);
    }
}

