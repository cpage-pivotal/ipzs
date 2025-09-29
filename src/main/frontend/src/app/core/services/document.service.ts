import { Injectable, Inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DOCUMENT } from '@angular/common';
import { Observable } from 'rxjs';
import { DocumentMetadata } from '../models/document.model';

@Injectable({
  providedIn: 'root'
})
export class DocumentService {
  private readonly baseUrl: string;

  constructor(
    private http: HttpClient,
    @Inject(DOCUMENT) private document: Document
  ) {
    this.baseUrl = this.getApiBaseUrl() + '/api/documents';
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

  getAllDocuments(): Observable<DocumentMetadata[]> {
    return this.http.get<DocumentMetadata[]>(this.baseUrl);
  }
}