
package com.reviewlens.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @Column(name = "review_id", nullable = false, unique = true)
    private String reviewId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "review_text", nullable = false, columnDefinition = "TEXT")
    private String reviewText;

    @Column(name = "review_date")
    private LocalDate reviewDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_status", nullable = false,
            length = 30)
    private AnalysisStatus analysisStatus = AnalysisStatus.PENDING;

    public Review() {
    }

    public Review(String reviewId, String productName,
                  Integer rating, String reviewText,
                  LocalDate reviewDate) {

        this.reviewId = reviewId;
        this.productName = productName;
        this.rating = rating;
        this.reviewText = reviewText;
        this.reviewDate = reviewDate;
    }

    public String getReviewId() {
        return reviewId;
    }

    public void setReviewId(String reviewId) {
        this.reviewId = reviewId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getReviewText() {
        return reviewText;
    }

    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }

    public LocalDate getReviewDate() {
        return reviewDate;
    }

    public void setReviewDate(LocalDate reviewDate) {
        this.reviewDate = reviewDate;
    }

    public AnalysisStatus getAnalysisStatus() {
        return analysisStatus == null ? AnalysisStatus.PENDING : analysisStatus;
    }

    public void setAnalysisStatus(AnalysisStatus analysisStatus) {
        this.analysisStatus = analysisStatus;
    }
}
