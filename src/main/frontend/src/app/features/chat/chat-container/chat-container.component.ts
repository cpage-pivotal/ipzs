import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MaterialModule } from '../../../shared/material/material.module';
import { ChatService } from '../../../core/services/chat.service';
import { ChatInputComponent } from '../chat-input/chat-input.component';
import { ChatMessagesComponent } from '../chat-messages/chat-messages.component';
import { DateSelectorComponent } from '../date-selector/date-selector.component';
import { TranslationService } from '../../../services/translation.service';

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
  templateUrl: './chat-container.component.html',
  styleUrls: ['./chat-container.component.scss']
})
export class ChatContainerComponent {
  isLoading = signal(false);

  constructor(
    public chatService: ChatService,
    public translationService: TranslationService
  ) {}

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