import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DocumentMetadata } from '../models/document.model';

@Injectable({
  providedIn: 'root'
})
export class DocumentService {
  private readonly baseUrl = '/api/documents';

  constructor(private http: HttpClient) {}

  getAllDocuments(): Observable<DocumentMetadata[]> {
    return this.http.get<DocumentMetadata[]>(this.baseUrl);
  }
}