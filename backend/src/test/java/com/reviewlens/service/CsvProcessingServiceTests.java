package com.reviewlens.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.reviewlens.dto.CsvProcessingResult;

class CsvProcessingServiceTests {

    private final CsvProcessingService service = new CsvProcessingService();

    @Test
    void cleansRowsAndCountsInvalidAndDuplicateRecords() throws Exception {
        String csv = "review_id,product,rating,date,review_text\n"
                + "r1,Phone A,nope,bad-date,Great battery\n"
                + "r2,Phone B,5,2025-01-02,Great battery\n"
                + "r2,Phone C,4,2025-01-03,Another review\n"
                + "r4,Phone A,4,2025-01-04,  GREAT   BATTERY  \n"
                + ",,5,2025-01-05,   \n"
                + " ,Phone A,3,2025-01-06,Review with missing ID\n"
                + "r5, ,3,2025-01-07,Review with missing product\n";

        CsvProcessingResult result = service.processCsv(file(csv));

        assertEquals(7, result.getTotalRows());
        assertEquals(2, result.getValidReviews());
        assertEquals(5, result.getSkippedReviews());
        assertEquals(2, result.getDuplicateReviews());
        assertEquals(1, result.getDuplicateReviewIds());
        assertEquals(2, result.getInvalidReviews());
        assertNull(result.getReviews().get(0).getRating());
        assertNull(result.getReviews().get(0).getReviewDate());
        assertEquals("Phone A", result.getReviews().get(0).getProductName());
        assertEquals("Phone B", result.getReviews().get(1).getProductName());
    }

    @Test
    void rejectsFilesMissingRequiredColumns() {
        assertThrows(IllegalArgumentException.class,
                () -> service.processCsv(file("review_id,review_text\nr1,ok\n")));
    }

    private MockMultipartFile file(String value) {
        return new MockMultipartFile("file", "reviews.csv", "text/csv",
                value.getBytes(StandardCharsets.UTF_8));
    }
}
