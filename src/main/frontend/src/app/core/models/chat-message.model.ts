export interface ChatMessage {
  type: 'user' | 'assistant';
  content: string;
  timestamp?: Date;
}

export interface ChatRequest {
  message: string;
  dateContext: Date;
  sessionId?: string;
}

export interface ChatResponse {
  message: string;
  sessionId: string;
  sources: string[];
}