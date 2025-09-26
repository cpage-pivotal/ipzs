package org.tanzu.ipzs.legislation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tanzu.ipzs.legislation.service.SampleDocumentService;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "http://localhost:4200")
public class SampleDataTestController {

    @Autowired
    private SampleDocumentService sampleDocumentService;

    /**
     * Simple synchronous endpoint to test sample data generation
     */
    @PostMapping("/generate-sample")
    public ResponseEntity<String> generateSampleDataSync() {
        try {
            var future = sampleDocumentService.generateAndIngestSampleDocuments();
            var results = future.get(); // Block for testing

            var successCount = results.stream()
                    .mapToInt(result -> result.success() ? 1 : 0)
                    .sum();

            var totalChunks = results.stream()
                    .mapToInt(SampleDocumentService.IngestionResult::chunksCreated)
                    .sum();

            return ResponseEntity.ok(
                    String.format("Generated %d documents successfully with %d total chunks",
                            successCount, totalChunks)
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error generating sample data: " + e.getMessage());
        }
    }

    /**
     * Check if sample data generation is working
     */
    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok("Sample data test controller is ready");
    }
}