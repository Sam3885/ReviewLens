
package com.reviewlens.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.reviewlens.dto.CsvProcessingResult;
import com.reviewlens.service.CsvProcessingService;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "http://localhost:5173")
public class CsvUploadController {

    private final CsvProcessingService csvProcessingService;

    public CsvUploadController(
            CsvProcessingService csvProcessingService) {
        this.csvProcessingService = csvProcessingService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadCsv(
            @RequestParam("file") MultipartFile file) {

        try {

            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("Please upload a CSV file.");
            }

            if (!file.getOriginalFilename()
                    .toLowerCase().endsWith(".csv")) {
                return ResponseEntity.badRequest()
                        .body("Only CSV files are allowed.");
            }

            CsvProcessingResult result =
                    csvProcessingService.processCsv(file);

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());

        } catch (IOException e) {

            return ResponseEntity.status(
                    HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing CSV file.");
        }
    }
}
