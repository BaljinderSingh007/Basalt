/**
 * Domain models shared across the Basalt frontend.
 */

export type MessageRole = 'user' | 'assistant';

export interface ChatMessage {
  id: string;
  role: MessageRole;
  content: string;
  /** ISO timestamp */
  timestamp: Date;
  /** True while the assistant is still streaming tokens */
  isStreaming?: boolean;
  /** Populated when the assistant returns an image URL */
  imageUrl?: string;
}

export interface ChatRequest {
  message: string;
  conversationId?: string;
  useRag?: boolean;
}

export interface ImageGenerationRequest {
  prompt: string;
  width?: number;
  height?: number;
  seed?: number;
}

export interface ImageGenerationResponse {
  imageUrl: string;
}

