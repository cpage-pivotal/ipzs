export interface DocumentMetadata {
  documentId: string;
  title: string;
  documentType: string;
  publicationDate: string;
  effectiveDate: string;
  expirationDate?: string;
  issuingAuthority?: string;
  documentNumber?: string;
  keyProvisions?: string[];
  supersedes?: string[];
  supersededBy?: string;
}

export interface DocumentStats {
  totalDocuments: number;
  currentDocuments: number;
  expiredDocuments: number;
  futureDocuments: number;
}

export interface GenerationResult {
  totalDocuments: number;
  successCount: number;
  failureCount: number;
  totalChunks: number;
  details: IngestionResult[];
}

export interface IngestionResult {
  documentId: string;
  chunksCreated: number;
  success: boolean;
  message: string;
}

export interface DocumentQueryParams {
  documentType?: string;
  issuingAuthority?: string;
  effectiveDate?: string;
  startDate?: string;
  endDate?: string;
  query?: string;
}