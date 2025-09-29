export interface ChatMessage {
  type: 'user' | 'assistant';
  content: string;
  timestamp?: Date;
}

export interface ChatRequest {
  message: string;
  dateContext: string; // ISO date string to match backend LocalDate
  sessionId?: string;
}

export interface ChatResponse {
  message: string;
  sessionId: string;
  sources: string[];
}