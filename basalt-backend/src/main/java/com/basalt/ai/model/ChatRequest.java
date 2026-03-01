package com.basalt.ai.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Inbound chat request payload from the Angular frontend.
 */
@Data
public class ChatRequest {

    /** The user's message text. */
    @NotBlank(message = "message must not be blank")
    private String message;

    /**
     * Optional conversation ID for multi-turn context.
     * Null for a fresh conversation.
     */
    private String conversationId;

    /**
     * When {@code true}, the RAG pipeline enriches the prompt with
     * relevant document context retrieved from PgVector.
     */
    private boolean useRag = false;
}

