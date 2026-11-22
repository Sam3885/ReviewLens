
package com.reviewlens.service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.reviewlens.dto.CsvProcessingResult;
import com.reviewlens.entity.Review;

@Service
public class CsvProcessingService {

    public CsvProcessingResult processCsv(MultipartFile file)
            throws IOException {

        List<Review> reviews = new ArrayList<>();

        Set<String> seenReviewTexts = new HashSet<>();

        int totalRows = 0;
        int skippedReviews = 0;
        int duplicateReviews = 0;

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .get();

        try (Reader reader = new InputStreamReader(
                    file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {

            // Validate required CSV columns
            Set<String> headers = parser.getHeaderMap().keySet();

            if (!headers.containsAll(Set.of(
                    "review_id", "product", "rating",
                    "date", "review_text"))) {
                throw new IllegalArgumentException(
                        "CSV is missing one or more required columns.");
            }

            for (CSVRecord record : parser) {

                totalRows++;

                String reviewId = record.get("review_id").strip();
                String product = record.get("product").strip();
                String ratingText = record.get("rating").strip();
                String reviewText = record.get("review_text").strip();
                String dateText = record.get("date").strip();

                // Skip empty review text
                if (reviewText.isEmpty()) {
                    skippedReviews++;
                    continue;
                }

                // Normalize text for duplicate detection
                String normalizedText = reviewText
                        .replaceAll("\\s+", " ")
                        .toLowerCase(Locale.ROOT);

                // Detect duplicate reviews
                if (!seenReviewTexts.add(normalizedText)) {
                    duplicateReviews++;
                    continue;
                }

                // Parse rating; invalid or missing means null
                Integer rating = null;

                try {
                    int parsedRating = Integer.parseInt(ratingText);

                    if (parsedRating >= 1 && parsedRating <= 5) {
                        rating = parsedRating;
                    }

                } catch (NumberFormatException exception) {
                    rating = null;
                }

                // Parse date safely
                LocalDate reviewDate = null;

                try {
                    reviewDate = LocalDate.parse(dateText);
                } catch (Exception exception) {
                    reviewDate = null;
                }

                Review review = new Review(
                        reviewId,
                        product,
                        rating,
                        reviewText,
                        reviewDate
                );

                reviews.add(review);
            }
        }

        return new CsvProcessingResult(
                reviews,
                totalRows,
                reviews.size(),
                skippedReviews,
                duplicateReviews
        );
    }
}
