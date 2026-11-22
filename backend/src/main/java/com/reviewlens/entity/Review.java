
package com.reviewlens.entity;

import java.time.LocalDate;

public class Review {

    private String reviewId;
    private String productName;
    private Integer rating;
    private String reviewText;
    private LocalDate reviewDate;

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
}
