package org.tanzu.ipzs.legislation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tanzu.ipzs.legislation.service.SampleDocumentService;
import org.tanzu.ipzs.legislation.service.SampleDocumentService.IngestionResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:4200")
public class SampleDataController {

    @Autowired
    private SampleDocumentService sampleDocumentService;

    /**
     * Generate and ingest sample legislative documents into the vector store
     */
    @PostMapping("/generate-sample-data")
    public CompletableFuture<ResponseEntity<GenerationResponse>> generateSampleData() {
        return sampleDocumentService.generateAndIngestSampleDocuments()
                .thenApply(results -> {
                    var successCount = results.stream()
                            .mapToInt(result -> result.success() ? 1 : 0)
                            .sum();

                    var failureCount = results.size() - successCount;

                    var totalChunks = results.stream()
                            .mapToInt(IngestionResult::chunksCreated)
                            .sum();

                    var response = new GenerationResponse(
                            results.size(),
                            successCount,
                            failureCount,
                            totalChunks,
                            results
                    );

                    return ResponseEntity.ok(response);
                })
                .exceptionally(throwable -> {
                    var errorResponse = new GenerationResponse(
                            0, 0, 1, 0,
                            List.of(new IngestionResult("ERROR", 0, false, throwable.getMessage()))
                    );
                    return ResponseEntity.internalServerError().body(errorResponse);
                });
    }

    /**
     * Check the status of sample data generation
     */
    @GetMapping("/sample-data-status")
    public ResponseEntity<StatusResponse> getSampleDataStatus() {
        // This could be enhanced to track actual generation status
        // For now, just return a simple status
        return ResponseEntity.ok(new StatusResponse(
                "ready",
                "Sample data generation service is ready"
        ));
    }

    // Response records using modern Java
    public record GenerationResponse(
            int totalDocuments,
            int successCount,
            int failureCount,
            int totalChunks,
            List<IngestionResult> details
    ) {}

    public record StatusResponse(
            String status,
            String message
    ) {}
}