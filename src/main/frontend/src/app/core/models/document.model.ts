export interface DocumentMetadata {
  documentId: string;
  title: string;
  documentType: string;
  publicationDate: string;
  effectiveDate: string;
  expirationDate?: string;
  issuingAuthority?: string;
}