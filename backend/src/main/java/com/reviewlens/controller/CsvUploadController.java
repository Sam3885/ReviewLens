
package com.reviewlens.controller;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.reviewlens.dto.CsvProcessingResult;
import com.reviewlens.repository.ReviewRepository;
import com.reviewlens.service.CsvProcessingService;
import com.reviewlens.service.ReviewAnalysisService;

@RestController
@RequestMapping("/api/reviews")
public class CsvUploadController {

    private final CsvProcessingService csvProcessingService;
    private final ReviewAnalysisService reviewAnalysisService;
    private final ReviewRepository reviewRepository;

    public CsvUploadController(
            CsvProcessingService csvProcessingService,
            ReviewAnalysisService reviewAnalysisService,
            ReviewRepository reviewRepository) {

        this.csvProcessingService = csvProcessingService;
        this.reviewAnalysisService = reviewAnalysisService;
        this.reviewRepository = reviewRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadCsv(
            @RequestParam("file") MultipartFile file) {

        try {

            // Step 1: Validate uploaded file
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("Please upload a CSV file.");
            }

            String filename = file.getOriginalFilename();

            if (filename == null ||
                    !filename.toLowerCase().endsWith(".csv")) {

                return ResponseEntity.badRequest()
                        .body("Only CSV files are allowed.");
            }

            // Step 2: Process and clean CSV data
            CsvProcessingResult csvResult =
                    csvProcessingService.processCsv(file);

            // Step 3: Save cleaned reviews in PostgreSQL
            reviewRepository.saveAll(csvResult.getReviews());

            // Step 4: Analyze reviews using Gemini
            var analyses = reviewAnalysisService.analyzeReviews(
                    csvResult.getReviews()
            );

            // Step 5: Return combined response
            Map<String, Object> response = Map.of(
                    "csvProcessing", csvResult,
                    "savedReviews", csvResult.getReviews().size(),
                    "analyses", analyses
            );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());

        } catch (IOException e) {

            return ResponseEntity.status(
                    HttpStatus.INTERNAL_SERVER_ERROR
            ).body("Error processing CSV file.");

        } catch (IllegalStateException e) {

            return ResponseEntity.status(
                    HttpStatus.BAD_GATEWAY
            ).body("Review analysis failed: " + e.getMessage());
        }
    }
}
