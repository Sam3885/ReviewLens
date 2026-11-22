package com.reviewlens.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.reviewlens.dto.ReviewAnalysis;
import com.reviewlens.dto.ReviewView;
import com.reviewlens.dto.SuggestedReplyResponse;
import com.reviewlens.entity.Review;
import com.reviewlens.entity.ReviewAnalysisEntity;
import com.reviewlens.repository.ReviewAnalysisRepository;
import com.reviewlens.repository.ReviewRepository;

@Service
public class ReviewApiService {

    private static final Set<String> SENTIMENTS =
            Set.of("positive", "neutral", "negative");
    private static final Set<String> TOPICS = Set.of(
            "quality", "delivery", "price", "packaging", "support",
            "size_fit", "other");

    private final ReviewRepository reviewRepository;
    private final ReviewAnalysisRepository analysisRepository;
    private final GeminiService geminiService;

    public ReviewApiService(ReviewRepository reviewRepository,
                            ReviewAnalysisRepository analysisRepository,
                            GeminiService geminiService) {
        this.reviewRepository = reviewRepository;
        this.analysisRepository = analysisRepository;
        this.geminiService = geminiService;
    }

    @Transactional(readOnly = true)
    public Page<ReviewView> listReviews(int page, int size,
                                        String product, String sentiment,
                                        String topic) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be from 1 to 100.");
        }
        String normalizedSentiment = normalizeFilter(sentiment);
        String normalizedTopic = normalizeFilter(topic);
        if (!normalizedSentiment.isEmpty() && !SENTIMENTS.contains(normalizedSentiment)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported sentiment filter.");
        }
        if (!normalizedTopic.isEmpty() && !TOPICS.contains(normalizedTopic)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported topic filter.");
        }

        Page<Review> reviews = reviewRepository.findFiltered(
                normalizeFilter(product), normalizedSentiment, normalizedTopic,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "reviewDate")
                        .and(Sort.by(Sort.Direction.ASC, "reviewId"))));
        List<String> ids = reviews.getContent().stream().map(Review::getReviewId).toList();
        Map<String, ReviewAnalysis> analyses = analysisRepository
                .findByReview_ReviewIdIn(ids).stream()
                .filter(entity -> entity.getReview().getAnalysisStatus()
                        != com.reviewlens.entity.AnalysisStatus.ANALYSIS_FAILED)
                .collect(Collectors.toMap(
                        entity -> entity.getReview().getReviewId(),
                        this::toDto, (first, ignored) -> first));
        return reviews.map(review -> toView(review, analyses.get(review.getReviewId())));
    }

    @Transactional(readOnly = true)
    public ReviewView getReview(String reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Review not found."));
        ReviewAnalysis analysis = analysisRepository
                .findByReview_ReviewId(reviewId)
                .filter(entity -> review.getAnalysisStatus()
                        != com.reviewlens.entity.AnalysisStatus.ANALYSIS_FAILED)
                .map(this::toDto).orElse(null);
        return toView(review, analysis);
    }

    public SuggestedReplyResponse suggestReply(String reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Review not found."));
        ReviewAnalysisEntity analysis = analysisRepository
                .findByReview_ReviewId(reviewId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT, "Review has not been successfully analysed."));
        if (review.getAnalysisStatus()
                == com.reviewlens.entity.AnalysisStatus.ANALYSIS_FAILED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Review analysis failed; suggested reply is unavailable.");
        }
        if (!analysis.isNeedsReply()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A suggested reply is only available for reviews marked needs_reply.");
        }
        return new SuggestedReplyResponse(
                reviewId, geminiService.generateSuggestedReply(review));
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) return "";
        return value.strip().toLowerCase(Locale.ROOT);
    }

    private ReviewView toView(Review review, ReviewAnalysis analysis) {
        return new ReviewView(review.getReviewId(), review.getProductName(),
                review.getRating(), review.getReviewText(), review.getReviewDate(),
                review.getAnalysisStatus(), analysis);
    }

    private ReviewAnalysis toDto(ReviewAnalysisEntity entity) {
        return new ReviewAnalysis(entity.getSentiment(), entity.getTopics(),
                entity.getSummary(), entity.isNeedsReply());
    }
}
