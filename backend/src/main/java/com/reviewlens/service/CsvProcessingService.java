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

    private static final Set<String> REQUIRED_HEADERS = Set.of(
            "review_id", "product", "rating", "date", "review_text");

    public CsvProcessingResult processCsv(MultipartFile file)
            throws IOException {
        List<Review> reviews = new ArrayList<>();
        Set<String> seenReviewIds = new HashSet<>();
        Set<String> seenProductReviews = new HashSet<>();
        int totalRows = 0;
        int skippedReviews = 0;
        int duplicateReviews = 0;
        int invalidReviews = 0;
        int duplicateReviewIds = 0;

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .get();

        try (Reader reader = new InputStreamReader(
                    file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {
            if (!parser.getHeaderMap().keySet().containsAll(REQUIRED_HEADERS)) {
                throw new IllegalArgumentException(
                        "CSV is missing one or more required columns.");
            }

            for (CSVRecord record : parser) {
                totalRows++;
                String reviewText = value(record, "review_text");

                // Preserve the existing policy: blank review text is skipped.
                if (reviewText.isEmpty()) {
                    skippedReviews++;
                    continue;
                }

                String reviewId = value(record, "review_id");
                String product = value(record, "product");
                if (reviewId.isEmpty() || product.isEmpty()) {
                    skippedReviews++;
                    invalidReviews++;
                    continue;
                }

                if (!seenReviewIds.add(reviewId)) {
                    skippedReviews++;
                    duplicateReviews++;
                    duplicateReviewIds++;
                    continue;
                }

                String normalizedProduct = normalize(product);
                String normalizedText = normalize(reviewText);
                String contentKey = normalizedProduct + "\u001f" + normalizedText;
                if (!seenProductReviews.add(contentKey)) {
                    skippedReviews++;
                    duplicateReviews++;
                    continue;
                }

                Integer rating = parseRating(value(record, "rating"));
                LocalDate reviewDate = parseDate(value(record, "date"));
                reviews.add(new Review(
                        reviewId, product, rating, reviewText, reviewDate));
            }
        }

        return new CsvProcessingResult(
                reviews, totalRows, reviews.size(), skippedReviews,
                duplicateReviews, invalidReviews, duplicateReviewIds);
    }

    private String value(CSVRecord record, String column) {
        try {
            String value = record.get(column);
            return value == null ? "" : value.strip();
        } catch (IllegalArgumentException | IndexOutOfBoundsException exception) {
            return "";
        }
    }

    private String normalize(String value) {
        return value.replaceAll("\\s+", " ").strip().toLowerCase(Locale.ROOT);
    }

    private Integer parseRating(String value) {
        try {
            int rating = Integer.parseInt(value);
            return rating >= 1 && rating <= 5 ? rating : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
