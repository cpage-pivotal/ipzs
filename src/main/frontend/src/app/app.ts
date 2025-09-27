import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatToolbarModule, MatButtonModule, MatIconModule],
  template: `
    <mat-toolbar color="primary">
      <span>Assistente Legislativo IPZS</span>
      <span class="spacer"></span>
      <nav>
        <a mat-button routerLink="/chat" routerLinkActive="active">
          <mat-icon>chat</mat-icon>
          Chat
        </a>
        <a mat-button routerLink="/documents" routerLinkActive="active">
          <mat-icon>description</mat-icon>
          Documenti
        </a>
      </nav>
    </mat-toolbar>
    <main>
      <router-outlet></router-outlet>
    </main>
  `,
  styles: [`
    .spacer {
      flex: 1 1 auto;
    }
    nav a {
      margin-left: 8px;
    }
    nav a.active {
      background-color: rgba(255,255,255,0.1);
    }
    main {
      padding: 16px;
    }
  `],
  styleUrl: './app.scss'
})
export class App {}
