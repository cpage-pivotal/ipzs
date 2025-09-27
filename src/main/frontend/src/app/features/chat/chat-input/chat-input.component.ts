import { Component, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MaterialModule } from '../../../shared/material/material.module';
import { TranslationService } from '../../../services/translation.service';

@Component({
  selector: 'app-chat-input',
  standalone: true,
  imports: [CommonModule, FormsModule, MaterialModule],
  template: `
    <div class="chat-input-container">
      <mat-form-field appearance="outline" class="message-input">
        <mat-label>{{ translationService.t('chat.input.label') }}</mat-label>
        <input
          matInput
          [(ngModel)]="messageText"
          (keyup.enter)="onSendMessage()"
          [disabled]="disabled()"
          [placeholder]="translationService.t('chat.input.placeholder')">
      </mat-form-field>
      <button
        mat-raised-button
        color="primary"
        (click)="onSendMessage()"
        [disabled]="disabled() || !messageText.trim()"
        class="send-button">
        <mat-icon>send</mat-icon>
        {{ translationService.t('chat.input.send') }}
      </button>
    </div>
  `,
  styles: [`
    .chat-input-container {
      display: flex;
      gap: 12px;
      align-items: flex-end;
      padding: 16px;
    }

    .message-input {
      flex: 1;
    }

    .send-button {
      white-space: nowrap;
    }
  `]
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