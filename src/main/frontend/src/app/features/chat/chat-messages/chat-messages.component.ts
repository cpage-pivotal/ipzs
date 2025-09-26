import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MaterialModule } from '../../../shared/material/material.module';
import { ChatMessage } from '../../../core/models/chat-message.model';

@Component({
  selector: 'app-chat-messages',
  standalone: true,
  imports: [CommonModule, MaterialModule],
  template: `
    <div class="messages-container">
      @for (message of messages(); track $index) {
        <div class="message-wrapper" [ngClass]="'message-' + message.type">
          <mat-card class="message-card" [ngClass]="message.type">
            <mat-card-content>
              <div class="message-content">{{ message.content }}</div>
              @if (message.timestamp) {
                <div class="message-timestamp">
                  {{ message.timestamp | date:'short':'it-IT' }}
                </div>
              }
            </mat-card-content>
          </mat-card>
        </div>
      }

      @if (messages().length === 0) {
        <div class="welcome-message">
          <mat-card>
            <mat-card-content>
              <h3>Benvenuto nell'Assistente Legislativo IPZS</h3>
              <p>Puoi farmi domande sulla legislazione italiana. Seleziona una data di riferimento e inizia a chattare!</p>
            </mat-card-content>
          </mat-card>
        </div>
      }
    </div>
  `,
  styles: [`
    .messages-container {
      height: 400px;
      overflow-y: auto;
      padding: 16px;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .message-wrapper {
      display: flex;
    }

    .message-wrapper.message-user {
      justify-content: flex-end;
    }

    .message-wrapper.message-assistant {
      justify-content: flex-start;
    }

    .message-card {
      max-width: 70%;
      margin: 0;
    }

    .message-card.user {
      background-color: #e3f2fd;
    }

    .message-card.assistant {
      background-color: #f5f5f5;
    }

    .message-content {
      white-space: pre-wrap;
      word-wrap: break-word;
    }

    .message-timestamp {
      font-size: 0.75rem;
      color: #666;
      margin-top: 8px;
      text-align: right;
    }

    .welcome-message {
      display: flex;
      justify-content: center;
      align-items: center;
      height: 100%;
    }

    .welcome-message mat-card {
      text-align: center;
      max-width: 500px;
    }
  `]
})
export class ChatMessagesComponent {
  messages = input.required<ChatMessage[]>();
}