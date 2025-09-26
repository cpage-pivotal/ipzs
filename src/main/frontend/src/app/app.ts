import { Component } from '@angular/core';
import { ChatContainerComponent } from './features/chat/chat-container/chat-container.component';

@Component({
  selector: 'app-root',
  imports: [ChatContainerComponent],
  template: '<app-chat-container></app-chat-container>',
  styleUrl: './app.scss'
})
export class App {}
