import {
  Component,
  EventEmitter,
  Input,
  Output,
  ViewChild,
  ElementRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

export interface SendEvent {
  text: string;
  useRag: boolean;
  isImageRequest: boolean;
}

/**
 * ChatInputComponent
 *
 * Sticky bottom input bar styled after the Gemini UI.
 * Features:
 * - Auto-expanding textarea (Shift+Enter for newline, Enter to send)
 * - RAG toggle button
 * - Image generation toggle button (prefixes "Generate image: " to the prompt)
 * - Disabled state while the assistant is streaming
 */
@Component({
  selector: 'app-chat-input',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="w-full px-4 pb-4 pt-2">
      <div
        class="flex items-end gap-2 bg-basalt-surface rounded-2xl border border-basalt-border
               focus-within:border-basalt-accent transition-colors duration-200 px-4 py-3"
      >
        <!-- RAG toggle -->
        <button
          type="button"
          (click)="useRag = !useRag"
          [title]="useRag ? 'RAG: ON — using document context' : 'RAG: OFF'"
          class="flex-shrink-0 p-1.5 rounded-lg transition-colors"
          [ngClass]="useRag ? 'text-basalt-accent bg-basalt-surface2' : 'text-basalt-muted hover:text-basalt-text'"
        >
          <!-- Document icon -->
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414A1 1 0 0119 9.414V19a2 2 0 01-2 2z"/>
          </svg>
        </button>

        <!-- Image gen toggle -->
        <button
          type="button"
          (click)="isImageMode = !isImageMode"
          [title]="isImageMode ? 'Image mode: ON' : 'Image mode: OFF'"
          class="flex-shrink-0 p-1.5 rounded-lg transition-colors"
          [ngClass]="isImageMode ? 'text-purple-400 bg-basalt-surface2' : 'text-basalt-muted hover:text-basalt-text'"
        >
          <!-- Image icon -->
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"/>
          </svg>
        </button>

        <!-- Auto-growing textarea -->
        <textarea
          #inputEl
          [(ngModel)]="inputText"
          (input)="autoResize()"
          (keydown.enter)="onEnter($event)"
          [disabled]="disabled"
          [placeholder]="isImageMode ? 'Describe an image to generate…' : 'Ask Basalt anything…'"
          rows="1"
          class="flex-1 bg-transparent resize-none outline-none text-basalt-text placeholder-basalt-muted
                 text-sm leading-6 max-h-40 overflow-y-auto disabled:opacity-50"
        ></textarea>

        <!-- Send button -->
        <button
          type="button"
          (click)="send()"
          [disabled]="!inputText.trim() || disabled"
          class="flex-shrink-0 p-1.5 rounded-lg text-basalt-accent hover:text-basalt-accentHov
                 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 24 24" fill="currentColor">
            <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
          </svg>
        </button>
      </div>

      <!-- Mode indicators -->
      <div class="flex gap-3 mt-1.5 px-1">
        <span *ngIf="useRag" class="text-xs text-basalt-accent">📄 Document context active</span>
        <span *ngIf="isImageMode" class="text-xs text-purple-400">🎨 Image generation mode</span>
      </div>
    </div>
  `,
})
export class ChatInputComponent {
  @Output() messageSent = new EventEmitter<SendEvent>();
  @ViewChild('inputEl') inputEl!: ElementRef<HTMLTextAreaElement>;

  inputText = '';
  useRag = false;
  isImageMode = false;
  @Input() disabled = false;

  onEnter(event: Event): void {
    const ke = event as KeyboardEvent;
    if (!ke.shiftKey) {
      ke.preventDefault();
      this.send();
    }
  }

  send(): void {
    const text = this.inputText.trim();
    if (!text || this.disabled) return;

    this.messageSent.emit({
      text,
      useRag: this.useRag,
      isImageRequest: this.isImageMode,
    });

    this.inputText = '';
    this.resetHeight();
  }

  autoResize(): void {
    const el = this.inputEl?.nativeElement;
    if (!el) return;
    el.style.height = 'auto';
    el.style.height = `${Math.min(el.scrollHeight, 160)}px`;
  }

  private resetHeight(): void {
    const el = this.inputEl?.nativeElement;
    if (el) el.style.height = 'auto';
  }
}
