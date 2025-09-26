// document-management.component.ts
import { Component, computed, effect, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatChipsModule } from '@angular/material/chips';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';

// Modern TypeScript interfaces using string literal types
export interface DocumentMetadata {
  documentId: string;
  title: string;
  documentType: 'Federal Legislation' | 'Federal Regulation' | 'State Law' | 'Local Ordinance';
  publicationDate: string;
  effectiveDate: string;
  expirationDate?: string;
  issuingAuthority: string;
  documentNumber: string;
  keyProvisions: string[];
  supersedes?: string[];
  supersededBy?: string;
}

export interface IngestionResult {
  documentId: string;
  chunksCreated: number;
  success: boolean;
  message: string;
}

// Service using modern HTTP client and signals
export interface DocumentService {
  generateSampleDocuments(): Promise<IngestionResult[]>;
  getAllDocuments(): Promise<DocumentMetadata[]>;
  getDocumentsByDateRange(startDate: string, endDate: string): Promise<DocumentMetadata[]>;
  searchDocuments(query: string, contextDate?: string): Promise<DocumentMetadata[]>;
}

@Component({
  selector: 'app-document-management',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatListModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatInputModule,
    MatChipsModule,
    MatExpansionModule,
    DatePipe
  ],
  templateUrl: './app-document-management.component.html',
  styleUrl: './app-document-management.component.scss'
})
export class DocumentManagementComponent {
  private snackBar = inject(MatSnackBar);

  // Reactive signals for state management
  documents = signal<DocumentMetadata[]>([]);
  loading = signal(false);
  searchQuery = signal('');
  contextDate = signal<Date | null>(null);

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
    let filtered = this.documents();

    // Apply search filter
    const query = this.searchQuery().toLowerCase();
    if (query) {
      filtered = filtered.filter(doc =>
        doc.title.toLowerCase().includes(query) ||
        doc.documentType.toLowerCase().includes(query) ||
        doc.issuingAuthority.toLowerCase().includes(query) ||
        doc.keyProvisions.some(provision =>
          provision.toLowerCase().includes(query)
        )
      );
    }

    // Apply date context filter
    const contextDate = this.contextDate();
    if (contextDate) {
      const contextDateStr = contextDate.toISOString().split('T')[0];
      filtered = filtered.filter(doc =>
        doc.effectiveDate <= contextDateStr &&
        (!doc.expirationDate || doc.expirationDate > contextDateStr)
      );
    }

    return filtered.sort((a, b) =>
      new Date(b.effectiveDate).getTime() - new Date(a.effectiveDate).getTime()
    );
  });

  constructor() {
    // Effect to automatically refresh when needed
    effect(() => {
      if (this.documents().length === 0 && !this.loading()) {
        this.refreshDocuments();
      }
    });
  }

  async generateSampleData(): Promise<void> {
    this.loading.set(true);
    try {
      // This would call the actual DocumentService
      const results = await this.mockGenerateSampleDocuments();

      const successCount = results.filter(r => r.success).length;
      const failCount = results.filter(r => !r.success).length;

      let message = `Generated ${successCount} documents successfully`;
      if (failCount > 0) {
        message += `, ${failCount} failed`;
      }

      this.snackBar.open(message, 'Close', {
        duration: 5000,
        panelClass: successCount > 0 ? 'success-snackbar' : 'error-snackbar'
      });

      // Refresh the document list
      await this.refreshDocuments();
    } catch (error) {
      this.snackBar.open('Failed to generate sample documents', 'Close', {
        duration: 5000,
        panelClass: 'error-snackbar'
      });
    } finally {
      this.loading.set(false);
    }
  }

  async refreshDocuments(): Promise<void> {
    this.loading.set(true);
    try {
      // This would call the actual DocumentService
      const docs = await this.mockGetAllDocuments();
      this.documents.set(docs);
    } catch (error) {
      this.snackBar.open('Failed to load documents', 'Close', {
        duration: 5000,
        panelClass: 'error-snackbar'
      });
    } finally {
      this.loading.set(false);
    }
  }

  onSearchChange(): void {
    // The computed signal will automatically update filteredDocuments
  }

  onDateChange(): void {
    // The computed signal will automatically update filteredDocuments
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

  // Mock service methods (replace with actual HTTP service calls)
  private async mockGenerateSampleDocuments(): Promise<IngestionResult[]> {
    await new Promise(resolve => setTimeout(resolve, 2000)); // Simulate API call
    return [
      { documentId: 'hr-2024-001', chunksCreated: 12, success: true, message: 'Success' },
      { documentId: 'hr-2025-042', chunksCreated: 15, success: true, message: 'Success' },
      { documentId: 'dea-2024-001', chunksCreated: 18, success: true, message: 'Success' },
      { documentId: 'hr-2025-089', chunksCreated: 22, success: true, message: 'Success' },
      { documentId: 'dhs-2024-003', chunksCreated: 14, success: true, message: 'Success' },
      { documentId: 'hr-2025-156', chunksCreated: 25, success: true, message: 'Success' },
      { documentId: 'dot-2024-007', chunksCreated: 11, success: true, message: 'Success' },
      { documentId: 'faa-2025-023', chunksCreated: 13, success: true, message: 'Success' },
      { documentId: 'nps-2024-001', chunksCreated: 9, success: true, message: 'Success' },
      { documentId: 'nps-2025-012', chunksCreated: 16, success: true, message: 'Success' }
    ];
  }

  private async mockGetAllDocuments(): Promise<DocumentMetadata[]> {
    await new Promise(resolve => setTimeout(resolve, 1000)); // Simulate API call
    return [
      {
        documentId: 'hr-2024-001',
        title: 'Highway Speed Limit Modernization Act of 2024',
        documentType: 'Federal Legislation',
        publicationDate: '2023-12-15',
        effectiveDate: '2024-01-01',
        issuingAuthority: 'United States Congress',
        documentNumber: 'H.R. 2024-001',
        keyProvisions: ['75 mph rural interstate', '65 mph urban highway', '25 mph residential'],
        supersededBy: 'hr-2025-042'
      },
      {
        documentId: 'hr-2025-042',
        title: 'Automated Vehicle Speed Integration Act of 2025',
        documentType: 'Federal Legislation',
        publicationDate: '2025-08-15',
        effectiveDate: '2025-09-01',
        issuingAuthority: 'United States Congress',
        documentNumber: 'H.R. 2025-042',
        keyProvisions: ['85 mph autonomous vehicles', 'dynamic speed zones', 'supersedes 2024 Act'],
        supersedes: ['hr-2024-001']
      }
      // ... additional mock documents
    ];
  }
}
