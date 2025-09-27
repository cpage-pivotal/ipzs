import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: '/chat', pathMatch: 'full' },
  {
    path: 'chat',
    loadComponent: () => import('./features/chat/chat-container/chat-container.component').then(m => m.ChatContainerComponent)
  },
  {
    path: 'documents',
    loadComponent: () => import('../app-document-management/app-document-management').then(m => m.DocumentManagementComponent)
  }
];
