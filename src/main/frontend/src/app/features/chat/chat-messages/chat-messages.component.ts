import { Component, input } from '@angular/core';
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
export class ChatMessagesComponent {
  messages = input.required<ChatMessage[]>();

  constructor(public translationService: TranslationService) {}
}