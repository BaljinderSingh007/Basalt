package com.basalt.ai.controller;

import com.basalt.ai.service.DocumentIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * REST controller for ingesting documents into the RAG vector store.
 *
 * <h3>Endpoints:</h3>
 * <ul>
 *   <li>{@code POST /api/documents/upload/pdf} — upload a PDF file</li>
 *   <li>{@code POST /api/documents/upload/text} — submit raw text content</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentIngestionService ingestionService;

    /**
     * Accepts a PDF file upload and ingests it into PgVector.
     */
    @PostMapping("/upload/pdf")
    public ResponseEntity<Map<String, String>> uploadPdf(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }
        try {
            ingestionService.ingestPdf(file);
            return ResponseEntity.ok(Map.of(
                    "status", "ingested",
                    "filename", file.getOriginalFilename()
            ));
        } catch (IOException e) {
            log.error("PDF ingestion failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Ingestion failed: " + e.getMessage()));
        }
    }

    /**
     * Accepts raw text and ingests it into PgVector.
     */
    @PostMapping("/upload/text")
    public ResponseEntity<Map<String, String>> uploadText(
            @RequestParam("content") String content,
            @RequestParam(value = "filename", defaultValue = "text-document") String filename) {
        ingestionService.ingestText(content, filename);
        return ResponseEntity.ok(Map.of("status", "ingested", "filename", filename));
    }
}

