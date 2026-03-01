package com.basalt.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Basalt AI Assistant — Backend Entry Point.
 *
 * <p>Spring Boot 3.2 application wiring together:
 * <ul>
 *   <li>Spring AI / Ollama for local LLM inference (streaming)</li>
 *   <li>PgVector for RAG document retrieval</li>
 *   <li>Pollinations.ai proxy for free image generation</li>
 * </ul>
 */
@SpringBootApplication
public class BasaltApplication {

    public static void main(String[] args) {
        SpringApplication.run(BasaltApplication.class, args);
    }
}

