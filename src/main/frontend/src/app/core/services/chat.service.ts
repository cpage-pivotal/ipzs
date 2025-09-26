import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { ChatMessage, ChatRequest, ChatResponse } from '../models/chat-message.model';

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private readonly baseUrl = '/api/chat';
  private messagesSignal = signal<ChatMessage[]>([]);
  private dateContextSignal = signal<Date>(new Date());
  private sessionId?: string;

  public messages = computed(() => this.messagesSignal());
  public dateContext = computed(() => this.dateContextSignal());

  constructor(private http: HttpClient) {}

  sendMessage(content: string): Observable<ChatResponse> {
    const userMessage: ChatMessage = {
      type: 'user',
      content,
      timestamp: new Date()
    };

    this.messagesSignal.update(messages => [...messages, userMessage]);

    const request: ChatRequest = {
      message: content,
      dateContext: this.dateContextSignal(),
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