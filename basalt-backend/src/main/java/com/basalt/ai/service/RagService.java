package com.basalt.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG (Retrieval-Augmented Generation) service.
 *
 * <p>Retrieves semantically similar document chunks from PgVector and
 * assembles them into a context block that is injected into the LLM prompt.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final VectorStore vectorStore;

    /** Number of document chunks to retrieve per query. */
    private static final int TOP_K = 5;

    /** Minimum similarity score to include a chunk (0.0 – 1.0). */
    private static final double SIMILARITY_THRESHOLD = 0.70;

    /**
     * Performs a similarity search against PgVector and returns a
     * formatted context block ready for prompt injection.
     *
     * @param query the user's raw query text
     * @return formatted string of retrieved document excerpts, or empty string
     *         if no relevant documents are found
     */
    public String retrieveContext(String query) {
        log.debug("RAG similarity search for query: {}", query);

        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(TOP_K)
                        .similarityThreshold(SIMILARITY_THRESHOLD)
                        .build()
        );

        if (results.isEmpty()) {
            log.debug("No relevant documents found above threshold {}", SIMILARITY_THRESHOLD);
            return "";
        }

        String context = results.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        log.debug("Retrieved {} document chunks for RAG context", results.size());
        return """
                Use the following context excerpts to inform your answer.
                If the context is insufficient, rely on your own knowledge.
                
                CONTEXT:
                %s
                
                """.formatted(context);
    }
}

