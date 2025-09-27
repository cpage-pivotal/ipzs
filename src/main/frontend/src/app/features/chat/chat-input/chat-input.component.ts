import { Component, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MaterialModule } from '../../../shared/material/material.module';
import { TranslationService } from '../../../services/translation.service';

@Component({
  selector: 'app-chat-input',
  standalone: true,
  imports: [CommonModule, FormsModule, MaterialModule],
  templateUrl: './chat-input.component.html',
  styleUrls: ['./chat-input.component.scss']
})
export class ChatInputComponent {
  messageText = '';
  disabled = input<boolean>(false);

  sendMessage = output<string>();

  constructor(public translationService: TranslationService) {}

  onSendMessage(): void {
    if (this.messageText.trim()) {
      this.sendMessage.emit(this.messageText.trim());
      this.messageText = '';
    }
  }
}