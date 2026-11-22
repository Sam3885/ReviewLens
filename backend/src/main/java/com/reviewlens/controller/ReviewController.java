package com.reviewlens.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.reviewlens.dto.ReviewView;
import com.reviewlens.dto.SuggestedReplyResponse;
import com.reviewlens.service.ReviewApiService;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewApiService reviewApiService;

    public ReviewController(ReviewApiService reviewApiService) {
        this.reviewApiService = reviewApiService;
    }

    @GetMapping
    public ResponseEntity<Page<ReviewView>> listReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String product,
            @RequestParam(required = false) String sentiment,
            @RequestParam(required = false) String topic) {
        return ResponseEntity.ok(reviewApiService.listReviews(
                page, size, product, sentiment, topic));
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewView> getReview(@PathVariable String reviewId) {
        return ResponseEntity.ok(reviewApiService.getReview(reviewId));
    }

    @PostMapping("/{reviewId}/suggested-reply")
    public ResponseEntity<SuggestedReplyResponse> suggestReply(
            @PathVariable String reviewId) {
        return ResponseEntity.ok(reviewApiService.suggestReply(reviewId));
    }
}
