import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ChatMessage,
  ChatRequest,
  ImageGenerationRequest,
  ImageGenerationResponse,
} from '../models/chat.model';

/**
 * ChatService — the core data layer for all Basalt AI interactions.
 *
 * Uses the native {@link EventSource} API to consume the Spring Boot
 * Server-Sent Events stream from {@code POST /api/chat/stream}.
 *
 * Because {@code EventSource} only supports GET requests natively,
 * we use a lightweight workaround: open a POST via fetch() with
 * {@code ReadableStream} body parsing, then emit tokens via an RxJS Subject.
 */
@Injectable({ providedIn: 'root' })
export class ChatService {

  constructor(private http: HttpClient) {}

  /**
   * Opens an SSE stream to the backend and emits each token via the
   * returned {@link Subject}. The caller should subscribe and append
   * tokens to the current message bubble.
   *
   * Emits {@code '[DONE]'} as the terminal sentinel, after which the
   * Subject completes.
   *
   * @param request the chat payload
   * @returns Subject that emits raw token strings
   */
  streamChat(request: ChatRequest): Subject<string> {
    const subject = new Subject<string>();

    fetch(environment.chatStreamUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream' },
      body: JSON.stringify(request),
    })
      .then((response) => {
        if (!response.ok || !response.body) {
          subject.error(new Error(`Stream failed: HTTP ${response.status}`));
          return;
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        const pump = (): void => {
          reader
            .read()
            .then(({ done, value }) => {
              if (done) {
                subject.complete();
                return;
              }

              // Accumulate decoded text so partial SSE frames aren't lost
              buffer += decoder.decode(value, { stream: true });
              const parts = buffer.split('\n');
              // Last element may be an incomplete line — keep it in the buffer
              buffer = parts.pop() ?? '';

              for (const line of parts) {
                if (line.startsWith('data:')) {
                  const token = line.slice(5); // preserve leading spaces
                  const trimmed = token.trim();
                  if (trimmed === '[DONE]') {
                    subject.complete();
                    return;
                  }
                  if (trimmed) {
                    subject.next(token);
                  }
                }
              }

              pump();
            })
            .catch((err) => subject.error(err));
        };

        pump();
      })
      .catch((err) => subject.error(err));

    return subject;
  }

  /**
   * Calls the AI Horde image-generation endpoint and returns the generated image URL.
   */
  generateImage(request: ImageGenerationRequest): Observable<ImageGenerationResponse> {
    return this.http.post<ImageGenerationResponse>(
      environment.imageGenUrl,
      request
    );
  }
}

