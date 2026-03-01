import {
  Component,
  Input,
  OnChanges,
  SimpleChanges,
  ViewEncapsulation,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MarkdownModule } from 'ngx-markdown';
import { ChatMessage } from '../../../../core/models/chat.model';

/**
 * MessageBubbleComponent
 *
 * Renders a single chat message bubble — either user (right-aligned, blue)
 * or assistant (left-aligned, dark surface). Assistant messages are parsed
 * as Markdown with syntax-highlighted code blocks via ngx-markdown +
 * highlight.js.
 */
@Component({
  selector: 'app-message-bubble',
  standalone: true,
  imports: [CommonModule, MarkdownModule],
  encapsulation: ViewEncapsulation.None,
  template: `
    <div
      class="flex w-full mb-4 px-4"
      [class.justify-end]="message.role === 'user'"
      [class.justify-start]="message.role === 'assistant'"
    >
      <!-- Avatar icon -->
      <div
        *ngIf="message.role === 'assistant'"
        class="flex-shrink-0 w-8 h-8 rounded-full bg-basalt-accent flex items-center justify-center text-basalt-bg font-bold text-sm mr-3 mt-1"
      >
        B
      </div>

      <!-- Bubble -->
      <div
        class="max-w-[78%] rounded-2xl px-4 py-3 text-sm leading-relaxed"
        [ngClass]="bubbleClass"
      >

        <!-- User message: plain text -->
        <ng-container *ngIf="message.role === 'user'">
          <p class="whitespace-pre-wrap text-basalt-text">{{ message.content }}</p>
        </ng-container>

        <!-- Assistant message: Markdown with syntax highlighting -->
        <ng-container *ngIf="message.role === 'assistant'">
          <!-- Image (when assistant returns an image URL) -->
          <ng-container *ngIf="message.imageUrl">
            <!-- Loading spinner (shown until image loads or errors) -->
            <div
              *ngIf="imageLoading && !imageLoadError"
              class="rounded-xl mb-3 border border-basalt-border bg-basalt-surface2 px-4 py-8 flex flex-col items-center gap-3"
            >
              <div class="w-8 h-8 border-2 border-basalt-accent border-t-transparent rounded-full animate-spin"></div>
              <span class="text-xs text-basalt-muted">Generating image… this may take 10–30 seconds</span>
            </div>

            <!-- The actual image (hidden while loading) -->
            <img
              *ngIf="!imageLoadError"
              [src]="message.imageUrl"
              alt="Generated image"
              class="rounded-xl max-w-full mb-3 border border-basalt-border"
              [class.hidden]="imageLoading"
              (load)="onImageLoad()"
              (error)="onImageError()"
            />

            <!-- Error state with retry -->
            <div
              *ngIf="imageLoadError"
              class="rounded-xl mb-3 border border-red-500/30 bg-red-500/10 px-4 py-4 text-sm text-red-400 flex flex-col items-center gap-2"
            >
              <span>⚠️ Image failed to load. The service may be temporarily unavailable.</span>
              <button
                (click)="retryImage()"
                class="px-3 py-1.5 rounded-lg text-xs bg-basalt-surface border border-basalt-border
                       text-basalt-text hover:border-basalt-accent hover:text-basalt-accent transition-colors"
              >
                🔄 Retry
              </button>
            </div>
          </ng-container>

          <!-- Markdown body -->
          <markdown
            [data]="message.content"
            class="prose prose-basalt max-w-none text-basalt-text"
          ></markdown>

          <!-- Streaming cursor -->
          <span
            *ngIf="message.isStreaming"
            class="inline-block w-1.5 h-4 bg-basalt-accent ml-0.5 animate-pulse"
          ></span>
        </ng-container>
      </div>

      <!-- User avatar -->
      <div
        *ngIf="message.role === 'user'"
        class="flex-shrink-0 w-8 h-8 rounded-full bg-basalt-surface2 flex items-center justify-center text-basalt-muted font-bold text-sm ml-3 mt-1"
      >
        U
      </div>
    </div>
  `,
})
export class MessageBubbleComponent implements OnChanges {
  @Input({ required: true }) message!: ChatMessage;

  bubbleClass = '';
  imageLoading = true;
  imageLoadError = false;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['message']) {
      this.imageLoading = true;
      this.imageLoadError = false;
      this.bubbleClass =
        this.message.role === 'user'
          ? 'bg-basalt-user text-basalt-text'
          : 'bg-basalt-ai text-basalt-text border border-basalt-border';
    }
  }

  onImageLoad(): void {
    this.imageLoading = false;
    this.imageLoadError = false;
  }

  onImageError(): void {
    this.imageLoading = false;
    this.imageLoadError = true;
  }

  /** Append a cache-busting param and reset state to force a reload. */
  retryImage(): void {
    if (!this.message.imageUrl) return;
    const sep = this.message.imageUrl.includes('?') ? '&' : '?';
    this.message.imageUrl = this.message.imageUrl.replace(/&_retry=\d+/, '')
      + `${sep}_retry=${Date.now()}`;
    this.imageLoading = true;
    this.imageLoadError = false;
  }
}

