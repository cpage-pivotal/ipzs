// document-management.component.ts
import { Component, computed, effect, signal, inject, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DatePipe } from '@angular/common';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { TranslationService } from '../app/services/translation.service';
import { DocumentService } from '../app/core/services/document.service';
import { DocumentMetadata, GenerationResult } from '../app/core/models/document.model';


@Component({
  selector: 'app-document-management',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatListModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatExpansionModule,
    DatePipe
  ],
  templateUrl: './app-document-management.component.html',
  styleUrl: './app-document-management.component.scss'
})
export class DocumentManagementComponent implements OnDestroy {
  private snackBar = inject(MatSnackBar);
  private documentService = inject(DocumentService);
  public translationService = inject(TranslationService);
  private destroy$ = new Subject<void>();

  // Reactive signals for state management
  documents = signal<DocumentMetadata[]>([]);
  loading = signal(false);
  hasLoaded = signal(false);

  // Computed signals for derived state
  totalDocuments = computed(() => this.documents().length);

  currentDocuments = computed(() => {
    const today = new Date().toISOString().split('T')[0];
    return this.documents().filter(doc =>
      doc.effectiveDate <= today &&
      (!doc.expirationDate || doc.expirationDate > today)
    ).length;
  });

  supersededDocuments = computed(() =>
    this.documents().filter(doc => doc.supersededBy).length
  );

  filteredDocuments = computed(() => {
    return this.documents().sort((a, b) =>
      new Date(b.effectiveDate).getTime() - new Date(a.effectiveDate).getTime()
    );
  });

  constructor() {
    // Effect to automatically refresh when needed
    effect(() => {
      if (this.documents().length === 0 && !this.loading() && !this.hasLoaded()) {
        this.refreshDocuments();
      }
    });
  }

  generateSampleData(): void {
    this.loading.set(true);

    this.documentService.generateSampleData()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result: GenerationResult) => {
          let message = `Generated ${result.successCount} documents successfully`;
          if (result.failureCount > 0) {
            message += `, ${result.failureCount} failed`;
          }

          this.snackBar.open(message, 'Close', {
            duration: 5000,
            panelClass: result.successCount > 0 ? 'success-snackbar' : 'error-snackbar'
          });

          // Refresh the document list
          this.refreshDocuments();
        },
        error: (error) => {
          console.error('Error generating sample documents:', error);
          this.snackBar.open('Failed to generate sample documents', 'Close', {
            duration: 5000,
            panelClass: 'error-snackbar'
          });
          this.loading.set(false);
        },
        complete: () => this.loading.set(false)
      });
  }

  refreshDocuments(): void {
    this.loading.set(true);
    this.hasLoaded.set(false);

    this.documentService.getAllDocuments()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (docs: DocumentMetadata[]) => {
          this.documents.set(docs);
          this.loading.set(false);
          this.hasLoaded.set(true);
        },
        error: (error) => {
          console.error('Error loading documents:', error);
          this.snackBar.open('Failed to load documents', 'Close', {
            duration: 5000,
            panelClass: 'error-snackbar'
          });
          this.loading.set(false);
          this.hasLoaded.set(true);
        }
      });
  }


  getDocumentStatusColor(doc: DocumentMetadata): string {
    if (doc.supersededBy) return 'warn';

    const today = new Date().toISOString().split('T')[0];
    if (doc.effectiveDate <= today && (!doc.expirationDate || doc.expirationDate > today)) {
      return 'primary';
    }

    return 'accent';
  }

  getDocumentIcon(doc: DocumentMetadata): string {
    if (doc.supersededBy) return 'history';

    const today = new Date().toISOString().split('T')[0];
    if (doc.effectiveDate <= today && (!doc.expirationDate || doc.expirationDate > today)) {
      return 'check_circle';
    }

    return 'schedule';
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
