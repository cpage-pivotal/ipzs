package org.tanzu.ipzs.legislation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tanzu.ipzs.legislation.model.dto.DocumentMetadataDto;
import org.tanzu.ipzs.legislation.model.entity.LegislationDocument;
import org.tanzu.ipzs.legislation.repository.LegislationDocumentRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "http://localhost:4200")
public class DocumentController {

    @Autowired
    private LegislationDocumentRepository documentRepository;

    /**
     * Get all documents with optional filtering
     */
    @GetMapping
    public ResponseEntity<List<DocumentMetadataDto>> getAllDocuments(
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) String issuingAuthority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDate) {

        List<LegislationDocument> documents;

        if (effectiveDate != null) {
            documents = documentRepository.findEffectiveDocuments(effectiveDate);
        } else if (documentType != null) {
            documents = documentRepository.findByDocumentType(documentType);
        } else if (issuingAuthority != null) {
            documents = documentRepository.findByIssuingAuthority(issuingAuthority);
        } else {
            documents = documentRepository.findAllByOrderByEffectiveDateDesc();
        }

        var dtos = documents.stream()
                .map(this::convertToDto)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * Get a specific document by its document ID
     */
    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentMetadataDto> getDocument(@PathVariable String documentId) {
        Optional<LegislationDocument> document = documentRepository.findByDocumentId(documentId);

        return document
                .map(doc -> ResponseEntity.ok(convertToDto(doc)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get documents effective within a date range
     */
    @GetMapping("/effective-range")
    public ResponseEntity<List<DocumentMetadataDto>> getDocumentsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        var documents = documentRepository.findByEffectiveDateBetween(startDate, endDate);
        var dtos = documents.stream()
                .map(this::convertToDto)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * Search documents by title
     */
    @GetMapping("/search")
    public ResponseEntity<List<DocumentMetadataDto>> searchDocuments(
            @RequestParam String query) {

        var documents = documentRepository.findByTitleContainingIgnoreCase(query);
        var dtos = documents.stream()
                .map(this::convertToDto)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * Get documents that are currently effective
     */
    @GetMapping("/current")
    public ResponseEntity<List<DocumentMetadataDto>> getCurrentDocuments() {
        var today = LocalDate.now();
        var documents = documentRepository.findEffectiveDocuments(today);
        var dtos = documents.stream()
                .map(this::convertToDto)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * Get expired documents
     */
    @GetMapping("/expired")
    public ResponseEntity<List<DocumentMetadataDto>> getExpiredDocuments() {
        var today = LocalDate.now();
        var documents = documentRepository.findExpiredDocuments(today);
        var dtos = documents.stream()
                .map(this::convertToDto)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * Get document statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<DocumentStatsDto> getDocumentStats() {
        var today = LocalDate.now();
        var totalCount = documentRepository.count();
        var currentCount = documentRepository.findEffectiveDocuments(today).size();
        var expiredCount = documentRepository.findExpiredDocuments(today).size();

        var stats = new DocumentStatsDto(
                totalCount,
                currentCount,
                expiredCount,
                totalCount - currentCount - expiredCount // future documents
        );

        return ResponseEntity.ok(stats);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Document service is running. Total documents: " + documentRepository.count());
    }

    /**
     * Convert entity to DTO using modern mapping
     */
    private DocumentMetadataDto convertToDto(LegislationDocument document) {
        return new DocumentMetadataDto(
                document.getDocumentId(),
                document.getTitle(),
                document.getDocumentType(),
                document.getPublicationDate(),
                document.getEffectiveDate(),
                document.getExpirationDate(),
                document.getIssuingAuthority()
        );
    }

    /**
     * Document statistics record
     */
    public record DocumentStatsDto(
            long totalDocuments,
            long currentDocuments,
            long expiredDocuments,
            long futureDocuments
    ) {}
}