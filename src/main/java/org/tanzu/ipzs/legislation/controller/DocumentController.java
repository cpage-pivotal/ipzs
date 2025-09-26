package org.tanzu.ipzs.legislation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tanzu.ipzs.legislation.model.dto.DocumentMetadataDto;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "http://localhost:4200")
public class DocumentController {

    @GetMapping
    public ResponseEntity<List<DocumentMetadataDto>> getAllDocuments() {
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/health")
    public String health() {
        return "Document service is running";
    }
}