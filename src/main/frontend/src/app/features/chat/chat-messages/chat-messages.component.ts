import { Component, input, ViewChild, ElementRef, AfterViewChecked, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MaterialModule } from '../../../shared/material/material.module';
import { ChatMessage } from '../../../core/models/chat-message.model';
import { TranslationService } from '../../../services/translation.service';

@Component({
  selector: 'app-chat-messages',
  standalone: true,
  imports: [CommonModule, MaterialModule],
  templateUrl: './chat-messages.component.html',
  styleUrls: ['./chat-messages.component.scss']
})
export class ChatMessagesComponent implements AfterViewChecked {
  @ViewChild('messagesContainer', { static: false }) private messagesContainer!: ElementRef;
  
  messages = input.required<ChatMessage[]>();
  private shouldScrollToBottom = true;

  constructor(public translationService: TranslationService) {
    // Effect to track when messages change
    effect(() => {
      const currentMessages = this.messages();
      if (currentMessages.length > 0) {
        this.shouldScrollToBottom = true;
      }
    });
  }

  ngAfterViewChecked(): void {
    if (this.shouldScrollToBottom) {
      this.scrollToBottom();
      this.shouldScrollToBottom = false;
    }
  }

  private scrollToBottom(): void {
    try {
      if (this.messagesContainer) {
        const element = this.messagesContainer.nativeElement;
        element.scrollTop = element.scrollHeight;
      }
    } catch (err) {
      console.error('Error scrolling to bottom:', err);
    }
  }
}