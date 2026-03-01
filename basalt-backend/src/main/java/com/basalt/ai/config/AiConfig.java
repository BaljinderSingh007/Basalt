package com.basalt.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI configuration.
 *
 * <p>Wires the {@link ChatClient} bean with Basalt's system persona so every
 * interaction inherits the Lead Software Engineer tone without requiring
 * callers to repeat it.
 */
@Configuration
public class AiConfig {

    private static final String SYSTEM_PROMPT = """
            You are Basalt, a Lead Software Engineer AI assistant.
            You are authoritative, precise, and focused on clean, scalable code.
            When answering technical questions, always prefer idiomatic solutions,
            cite trade-offs where relevant, and format code blocks with the correct
            language identifier. Keep prose concise — engineers value signal, not noise.
            """;

    /**
     * Builds a {@link ChatClient} pre-loaded with the Basalt system persona.
     *
     * @param ollamaChatModel auto-configured by spring-ai-ollama-spring-boot-starter
     */
    @Bean
    public ChatClient chatClient(OllamaChatModel ollamaChatModel) {
        return ChatClient.builder(ollamaChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }
}

