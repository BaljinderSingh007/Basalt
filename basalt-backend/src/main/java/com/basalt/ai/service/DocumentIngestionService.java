package com.basalt.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Document ingestion service.
 *
 * <p>Accepts uploaded files (PDF or plain text), chunks them with a
 * {@link TokenTextSplitter}, embeds each chunk via the configured
 * embedding model, and persists them to the PgVector store.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

    private final VectorStore vectorStore;

    /**
     * Ingests a PDF document into the vector store.
     *
     * @param file the uploaded PDF file
     * @throws IOException if the temporary file cannot be written
     */
    public void ingestPdf(MultipartFile file) throws IOException {
        log.info("Ingesting PDF: {}", file.getOriginalFilename());

        // Write to a temp file so the PDF reader can access it via filesystem URL
        Path tempFile = Files.createTempFile("basalt-ingest-", ".pdf");
        file.transferTo(tempFile.toFile());

        try {
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(
                    "file:" + tempFile.toAbsolutePath()
            );

            TokenTextSplitter splitter = new TokenTextSplitter(
                    800,   // chunk size (tokens)
                    400,   // overlap (tokens)
                    5,     // min chunk size
                    10_000, // max chunk size
                    true   // keep separator
            );

            List<Document> documents = splitter.apply(pdfReader.get());
            vectorStore.add(documents);

            log.info("Ingested {} chunks from PDF: {}", documents.size(), file.getOriginalFilename());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * Ingests a plain-text document into the vector store.
     *
     * @param content  raw text content
     * @param filename logical file name for metadata
     */
    public void ingestText(String content, String filename) {
        log.info("Ingesting text document: {}", filename);

        TokenTextSplitter splitter = new TokenTextSplitter(800, 400, 5, 10_000, true);
        List<Document> documents = splitter.apply(
                List.of(new Document(content))
        );
        vectorStore.add(documents);

        log.info("Ingested {} chunks from text document: {}", documents.size(), filename);
    }
}

