import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MaterialModule } from '../../../shared/material/material.module';
import { ChatService } from '../../../core/services/chat.service';
import { ChatInputComponent } from '../chat-input/chat-input.component';
import { ChatMessagesComponent } from '../chat-messages/chat-messages.component';
import { DateSelectorComponent } from '../date-selector/date-selector.component';

@Component({
  selector: 'app-chat-container',
  standalone: true,
  imports: [
    CommonModule,
    MaterialModule,
    ChatInputComponent,
    ChatMessagesComponent,
    DateSelectorComponent
  ],
  template: `
    <div class="chat-container">
      <mat-card class="chat-card">
        <mat-card-header>
          <div class="header-content">
            <div class="title-section">
              <mat-card-title>Assistente Legislativo IPZS</mat-card-title>
              <mat-card-subtitle>Sistema di consultazione legislativa intelligente</mat-card-subtitle>
            </div>
            <app-date-selector
              [currentDate]="chatService.dateContext()"
              (dateChange)="onDateChange($event)">
            </app-date-selector>
          </div>
        </mat-card-header>

        <mat-divider></mat-divider>

        <mat-card-content class="chat-content">
          <app-chat-messages
            [messages]="chatService.messages()">
          </app-chat-messages>
        </mat-card-content>

        <mat-divider></mat-divider>

        <mat-card-actions class="chat-actions">
          <app-chat-input
            (sendMessage)="onSendMessage($event)"
            [disabled]="isLoading()">
          </app-chat-input>
        </mat-card-actions>

        @if (isLoading()) {
          <div class="loading-overlay">
            <mat-spinner diameter="40"></mat-spinner>
          </div>
        }
      </mat-card>
    </div>
  `,
  styles: [`
    .chat-container {
      height: 100vh;
      display: flex;
      padding: 20px;
      background-color: #f5f5f5;
    }

    .chat-card {
      flex: 1;
      max-width: 1200px;
      margin: 0 auto;
      display: flex;
      flex-direction: column;
      position: relative;
    }

    .header-content {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      width: 100%;
    }

    .title-section {
      flex: 1;
    }

    .chat-content {
      flex: 1;
      padding: 0 !important;
    }

    .chat-actions {
      padding: 0 !important;
    }

    .loading-overlay {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background-color: rgba(255, 255, 255, 0.8);
      display: flex;
      justify-content: center;
      align-items: center;
      z-index: 1000;
    }
  `]
})
export class ChatContainerComponent {
  isLoading = signal(false);

  constructor(public chatService: ChatService) {}

  onSendMessage(message: string): void {
    this.isLoading.set(true);
    this.chatService.sendMessage(message).subscribe({
      next: () => {
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error sending message:', error);
        this.isLoading.set(false);
      }
    });
  }

  onDateChange(date: Date): void {
    this.chatService.setDateContext(date);
  }
}