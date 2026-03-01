# Basalt — System Architecture

## Phase 1: High-Level Component Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                         USER BROWSER                                   │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │              Angular 17 Frontend  (localhost:4200)               │  │
│  │                                                                  │  │
│  │  ChatComponent ──► ChatService ──► POST /api/chat/stream (SSE)   │  │
│  │  MessageBubble ◄── token Flux    ngx-markdown + highlight.js     │  │
│  │  ChatInput     ──► POST /api/images/generate  ──► ImageComponent │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
                             │  HTTP / SSE
                             ▼
┌────────────────────────────────────────────────────────────────────────┐
│                  Spring Boot 3.2 Backend  (localhost:8080)             │
│                                                                        │
│  ┌─────────────────┐   ┌──────────────────┐   ┌────────────────────┐  │
│  │  ChatController  │   │  ImageController  │   │ DocumentController │  │
│  │  POST /chat/     │   │  POST /images/    │   │ POST /documents/   │  │
│  │  stream → SSE    │   │  generate         │   │ upload/{pdf,text}  │  │
│  └────────┬─────────┘   └────────┬──────────┘   └────────┬───────────┘  │
│           │                      │                        │              │
│  ┌────────▼─────────┐            │              ┌────────▼───────────┐  │
│  │   ChatService     │            │              │  DocumentIngestion  │  │
│  │  buildPrompt()    │            │              │  Service           │  │
│  │  streamChat()     │            │              │  PDF → chunks →    │  │
│  └────────┬──────────┘            │              │  embeddings        │  │
│           │                      │              └────────┬───────────┘  │
│  ┌────────▼──────────┐            │                       │              │
│  │    RagService     │            │              ┌────────▼───────────┐  │
│  │  similaritySearch │            │              │   VectorStore      │  │
│  │  (optional)       │            │              │   (PgVectorStore)  │  │
│  └────────┬──────────┘            │              └────────────────────┘  │
│           │                      │                                       │
│  ┌────────▼──────────┐   ┌────────▼──────────┐                          │
│  │  Spring AI        │   │  WebClient /       │                          │
│  │  ChatClient       │   │  URL Builder       │                          │
│  │  (OllamaChatModel)│   │                    │                          │
│  └────────┬──────────┘   └────────┬──────────┘                          │
└───────────┼──────────────────────┼────────────────────────────────────┘
            │                      │
            ▼                      ▼
┌───────────────────┐   ┌───────────────────────────────────┐
│  Ollama           │   │  Pollinations.ai CDN               │
│  localhost:11434  │   │  https://image.pollinations.ai/    │
│  llama3.1 /       │   │  (free, no auth required)          │
│  deepseek-r1      │   └───────────────────────────────────┘
│  nomic-embed-text │
└───────────────────┘
            │
            ▼
┌───────────────────────────────────┐
│  PostgreSQL 16 + pgvector          │
│  localhost:5432 / basalt_db        │
│  vector_store table (HNSW index)   │
└───────────────────────────────────┘
```

---

## Data Flow: Standard Chat Request

```
1. User types message in Angular ChatInput
2. ChatComponent calls ChatService.streamChat({ message, useRag })
3. ChatService opens fetch() POST to /api/chat/stream with SSE accept header
4. Spring ChatController calls ChatService.streamChat()
5.   (if useRag=true) RagService.retrieveContext() queries PgVector
6.   Prompt = [optional RAG context] + user message
7. Spring AI ChatClient.prompt().user(prompt).stream().content() → Flux<String>
8. Flux is serialised as text/event-stream back to the browser
9. ChatService.pump() reads the ReadableStream, emits tokens via Subject<string>
10. ChatComponent appends each token to the live assistant message bubble
11. ngx-markdown re-renders Markdown on every token (with debounce in prod)
12. [DONE] sentinel closes the Subject, stops the streaming cursor
```

## Data Flow: RAG Document Ingestion

```
1. POST /api/documents/upload/pdf with multipart PDF
2. DocumentController → DocumentIngestionService.ingestPdf()
3. PagePdfDocumentReader parses PDF pages into Document objects
4. TokenTextSplitter chunks into 800-token segments with 400-token overlap
5. VectorStore.add() embeds each chunk via nomic-embed-text (Ollama) → PgVector
6. Chunks stored in vector_store table with HNSW index for fast ANN retrieval
```

## Data Flow: Image Generation

```
1. User enables image mode, types a prompt
2. ChatComponent calls ChatService.generateImage({ prompt, width, height })
3. POST /api/images/generate → ImageController.generateImage()
4. Controller URL-encodes the prompt and builds a Pollinations.ai URL
5. Returns { imageUrl: "https://image.pollinations.ai/prompt/..." }
6. Angular displays the image inline in a MessageBubble
```

---

## Technology Decisions

| Concern              | Choice                         | Rationale                                          |
|----------------------|--------------------------------|----------------------------------------------------|
| LLM inference        | Ollama (local)                 | Free, private, GPU-accelerated, multi-model        |
| Chat model           | llama3.1 / deepseek-r1         | Config-swappable via `spring.ai.ollama.chat.model` |
| Embedding model      | nomic-embed-text               | 768-dim, fast, purpose-built, free                 |
| Vector DB            | PostgreSQL + pgvector           | No extra infra; HNSW for sub-ms ANN search         |
| Image generation     | Pollinations.ai                | Zero cost, no auth, SDXL-quality images            |
| Streaming protocol   | Server-Sent Events (SSE)       | Simpler than WebSocket for unidirectional streams  |
| Markdown rendering   | ngx-markdown + highlight.js    | Battle-tested, tree-shakeable                      |
| Styling              | Tailwind CSS (dark, Gemini-ish)| Utility-first, zero runtime overhead              |

