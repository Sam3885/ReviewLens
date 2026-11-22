
package com.reviewlens.dto;

import java.util.List;
import com.reviewlens.entity.Review;

public class CsvProcessingResult {

    private List<Review> reviews;
    private int totalRows;
    private int validReviews;
    private int skippedReviews;
    private int duplicateReviews;
    private int invalidReviews;
    private int duplicateReviewIds;

    public CsvProcessingResult() {
    }

    public CsvProcessingResult(List<Review> reviews,
                               int totalRows,
                               int validReviews,
                               int skippedReviews,
                               int duplicateReviews) {
        this.reviews = reviews;
        this.totalRows = totalRows;
        this.validReviews = validReviews;
        this.skippedReviews = skippedReviews;
        this.duplicateReviews = duplicateReviews;
    }

    public CsvProcessingResult(List<Review> reviews,
                               int totalRows,
                               int validReviews,
                               int skippedReviews,
                               int duplicateReviews,
                               int invalidReviews,
                               int duplicateReviewIds) {
        this(reviews, totalRows, validReviews, skippedReviews, duplicateReviews);
        this.invalidReviews = invalidReviews;
        this.duplicateReviewIds = duplicateReviewIds;
    }

    public List<Review> getReviews() {
        return reviews;
    }

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getValidReviews() {
        return validReviews;
    }

    public void setValidReviews(int validReviews) {
        this.validReviews = validReviews;
    }

    public int getSkippedReviews() {
        return skippedReviews;
    }

    public void setSkippedReviews(int skippedReviews) {
        this.skippedReviews = skippedReviews;
    }

    public int getDuplicateReviews() {
        return duplicateReviews;
    }

    public void setDuplicateReviews(int duplicateReviews) {
        this.duplicateReviews = duplicateReviews;
    }

    public int getInvalidReviews() {
        return invalidReviews;
    }

    public void setInvalidReviews(int invalidReviews) {
        this.invalidReviews = invalidReviews;
    }

    public int getDuplicateReviewIds() {
        return duplicateReviewIds;
    }

    public void setDuplicateReviewIds(int duplicateReviewIds) {
        this.duplicateReviewIds = duplicateReviewIds;
    }
}
