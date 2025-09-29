import { Injectable, Inject } from '@angular/core';
import { HttpClient, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { DOCUMENT } from '@angular/common';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import {
  DocumentMetadata,
  DocumentStats,
  GenerationResult,
  DocumentQueryParams
} from '../models/document.model';

@Injectable({
  providedIn: 'root'
})
export class DocumentService {
  private readonly documentsUrl: string;
  private readonly adminUrl: string;

  constructor(
    private http: HttpClient,
    @Inject(DOCUMENT) private document: Document
  ) {
    const baseUrl = this.getApiBaseUrl();
    this.documentsUrl = `${baseUrl}/api/documents`;
    this.adminUrl = `${baseUrl}/api/admin`;
  }

  private getApiBaseUrl(): string {
    let host: string;
    let protocol: string;

    if (this.document.location.hostname === 'localhost') {
      host = 'localhost:8080';
    } else {
      host = this.document.location.host;
    }
    protocol = this.document.location.protocol;

    return `${protocol}//${host}`;
  }

  generateSampleData(): Observable<GenerationResult> {
    return this.http.post<GenerationResult>(
      `${this.adminUrl}/generate-sample-data`,
      {}
    ).pipe(
      catchError(this.handleError)
    );
  }

  getAllDocuments(params?: DocumentQueryParams): Observable<DocumentMetadata[]> {
    let httpParams = new HttpParams();
    if (params) {
      Object.entries(params).forEach(([key, value]) => {
        if (value) {
          httpParams = httpParams.set(key, value);
        }
      });
    }

    return this.http.get<DocumentMetadata[]>(this.documentsUrl, { params: httpParams })
      .pipe(catchError(this.handleError));
  }

  getDocument(documentId: string): Observable<DocumentMetadata> {
    return this.http.get<DocumentMetadata>(`${this.documentsUrl}/${documentId}`)
      .pipe(catchError(this.handleError));
  }

  searchDocuments(query: string): Observable<DocumentMetadata[]> {
    const params = new HttpParams().set('query', query);
    return this.http.get<DocumentMetadata[]>(`${this.documentsUrl}/search`, { params })
      .pipe(catchError(this.handleError));
  }

  getDocumentsByDateRange(startDate: Date, endDate: Date): Observable<DocumentMetadata[]> {
    const params = new HttpParams()
      .set('startDate', startDate.toISOString().split('T')[0])
      .set('endDate', endDate.toISOString().split('T')[0]);
    return this.http.get<DocumentMetadata[]>(`${this.documentsUrl}/effective-range`, { params })
      .pipe(catchError(this.handleError));
  }

  getCurrentDocuments(): Observable<DocumentMetadata[]> {
    return this.http.get<DocumentMetadata[]>(`${this.documentsUrl}/current`)
      .pipe(catchError(this.handleError));
  }

  getExpiredDocuments(): Observable<DocumentMetadata[]> {
    return this.http.get<DocumentMetadata[]>(`${this.documentsUrl}/expired`)
      .pipe(catchError(this.handleError));
  }

  getDocumentStats(): Observable<DocumentStats> {
    return this.http.get<DocumentStats>(`${this.documentsUrl}/stats`)
      .pipe(catchError(this.handleError));
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    console.error('DocumentService error:', error);

    let errorMessage = 'An error occurred while processing your request';

    if (error.error instanceof ErrorEvent) {
      // Client-side error
      errorMessage = `Client error: ${error.error.message}`;
    } else {
      // Server-side error
      if (error.error?.message) {
        errorMessage = error.error.message;
      } else if (error.status === 0) {
        errorMessage = 'Unable to connect to the server';
      } else if (error.status >= 400 && error.status < 500) {
        errorMessage = 'Invalid request or resource not found';
      } else if (error.status >= 500) {
        errorMessage = 'Server error occurred';
      }
    }

    return throwError(() => new Error(errorMessage));
  }
}