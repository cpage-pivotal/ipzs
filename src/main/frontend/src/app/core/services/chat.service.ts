import { Injectable, signal, computed, Inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { DOCUMENT } from '@angular/common';
import { Observable, tap, catchError, throwError } from 'rxjs';
import { ChatMessage, ChatRequest, ChatResponse } from '../models/chat-message.model';

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private readonly baseUrl: string;
  private messagesSignal = signal<ChatMessage[]>([]);
  private dateContextSignal = signal<Date>(new Date());
  private sessionId?: string;

  public messages = computed(() => this.messagesSignal());
  public dateContext = computed(() => this.dateContextSignal());

  constructor(
    private http: HttpClient,
    @Inject(DOCUMENT) private document: Document
  ) {
    this.baseUrl = this.getApiBaseUrl() + '/api/chat';
  }

  private getApiBaseUrl(): string {
    let host: string;
    let protocol: string;

    if (this.document.location.hostname === 'localhost') {
      host = 'localhost:8080';
    } else {
      host = this.document.location.host;
    }
    protocol = this.document.location.protocol;

    return `${protocol}//${host}`;
  }

  sendMessage(content: string): Observable<ChatResponse> {
    const userMessage: ChatMessage = {
      type: 'user',
      content,
      timestamp: new Date()
    };

    this.messagesSignal.update(messages => [...messages, userMessage]);

    const request: ChatRequest = {
      message: content,
      dateContext: this.dateContextSignal().toISOString().split('T')[0], // Convert Date to ISO date string (YYYY-MM-DD)
      sessionId: this.sessionId
    };

    return this.http.post<ChatResponse>(this.baseUrl, request).pipe(
      tap(response => {
        this.sessionId = response.sessionId;
        const assistantMessage: ChatMessage = {
          type: 'assistant',
          content: response.message,
          timestamp: new Date()
        };
        this.messagesSignal.update(messages => [...messages, assistantMessage]);
      }),
      catchError((error: HttpErrorResponse) => {
        console.error('Chat service error:', error);
        
        // Add error message to chat
        const errorMessage: ChatMessage = {
          type: 'assistant',
          content: `Sorry, I encountered an error: ${error.error?.message || error.message || 'Unknown error occurred'}`,
          timestamp: new Date()
        };
        this.messagesSignal.update(messages => [...messages, errorMessage]);
        
        return throwError(() => error);
      })
    );
  }

  setDateContext(date: Date): void {
    this.dateContextSignal.set(date);
  }

  clearMessages(): void {
    this.messagesSignal.set([]);
    this.sessionId = undefined;
  }
}