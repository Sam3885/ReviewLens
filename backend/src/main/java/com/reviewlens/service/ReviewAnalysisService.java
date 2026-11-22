package com.reviewlens.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.reviewlens.dto.ReviewAnalysis;
import com.reviewlens.entity.AnalysisStatus;
import com.reviewlens.entity.Review;
import com.reviewlens.entity.ReviewAnalysisEntity;
import com.reviewlens.repository.ReviewAnalysisRepository;
import com.reviewlens.repository.ReviewRepository;

@Service
public class ReviewAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewAnalysisService.class);
    private static final int BATCH_SIZE = 5;

    private final GeminiService geminiService;
    private final ReviewAnalysisRepository analysisRepository;
    private final ReviewRepository reviewRepository;
    private final Map<String, ReviewAnalysis> analysisCache = new ConcurrentHashMap<>();

    public ReviewAnalysisService(GeminiService geminiService,
                                 ReviewAnalysisRepository analysisRepository,
                                 ReviewRepository reviewRepository) {
        this.geminiService = geminiService;
        this.analysisRepository = analysisRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional
    public List<ReviewAnalysis> analyzeReviews(List<Review> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return new ArrayList<>();
        }

        List<ReviewAnalysis> results = new ArrayList<>(
                Collections.nCopies(reviews.size(), null));
        List<String> reviewIds = reviews.stream()
                .map(Review::getReviewId).toList();
        Map<String, Review> managedReviews = reviewRepository.findAllById(reviewIds)
                .stream().collect(Collectors.toMap(Review::getReviewId,
                        Function.identity(), (first, ignored) -> first,
                        LinkedHashMap::new));
        Map<String, ReviewAnalysisEntity> existingByReview =
                analysisRepository.findByReview_ReviewIdIn(reviewIds).stream()
                        .collect(Collectors.toMap(
                                entity -> entity.getReview().getReviewId(),
                                Function.identity(), (first, ignored) -> first));

        Map<String, List<Integer>> missingByHash = new LinkedHashMap<>();
        List<String> hashesToFetch = new ArrayList<>();
        List<String> keysByPosition = new ArrayList<>(reviews.size());

        for (int i = 0; i < reviews.size(); i++) {
            Review review = reviews.get(i);
            String hash = contentHash(review);
            keysByPosition.add(hash);
            ReviewAnalysisEntity existing = existingByReview.get(review.getReviewId());
            if (existing != null && matchesContent(existing, review, hash)) {
                ReviewAnalysis analysis = convertToDto(existing);
                results.set(i, copyAnalysis(analysis));
                existing.setContentHash(hash);
                analysisCache.put(hash, copyAnalysis(analysis));
                continue;
            }

            ReviewAnalysis cached = analysisCache.get(hash);
            if (cached != null) {
                results.set(i, copyAnalysis(cached));
            } else {
                missingByHash.computeIfAbsent(hash, ignored -> new ArrayList<>()).add(i);
                hashesToFetch.add(hash);
            }
        }

        if (!hashesToFetch.isEmpty()) {
            Map<String, ReviewAnalysis> persistedByHash =
                    analysisRepository.findByContentHashIn(hashesToFetch).stream()
                            .filter(entity -> entity.getContentHash() != null)
                            .collect(Collectors.toMap(
                                    ReviewAnalysisEntity::getContentHash,
                                    this::convertToDto,
                                    (first, ignored) -> first));
            for (Map.Entry<String, List<Integer>> entry : missingByHash.entrySet()) {
                ReviewAnalysis cached = persistedByHash.get(entry.getKey());
                if (cached == null) continue;
                analysisCache.put(entry.getKey(), copyAnalysis(cached));
                for (int position : entry.getValue()) {
                    results.set(position, copyAnalysis(cached));
                }
            }
        }

        List<String> unanalysedHashes = missingByHash.keySet().stream()
                .filter(hash -> results.get(missingByHash.get(hash).get(0)) == null)
                .toList();
        for (int start = 0; start < unanalysedHashes.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, unanalysedHashes.size());
            List<String> hashBatch = unanalysedHashes.subList(start, end);
            List<Review> reviewBatch = hashBatch.stream()
                    .map(hash -> reviews.get(missingByHash.get(hash).get(0)))
                    .toList();
            try {
                List<ReviewAnalysis> batchResults = geminiService.analyzeBatch(reviewBatch);
                if (batchResults == null || batchResults.size() != reviewBatch.size()) {
                    throw new IllegalStateException("Gemini returned a mismatched batch size.");
                }
                for (int offset = 0; offset < batchResults.size(); offset++) {
                    String hash = hashBatch.get(offset);
                    ReviewAnalysis analysis = batchResults.get(offset);
                    if (analysis == null) {
                        throw new IllegalStateException("Gemini returned a missing analysis.");
                    }
                    analysisCache.put(hash, copyAnalysis(analysis));
                    for (int position : missingByHash.get(hash)) {
                        results.set(position, copyAnalysis(analysis));
                    }
                }
            } catch (RuntimeException exception) {
                String diagnostic = exception instanceof GeminiService.GeminiFailureException failure
                        ? failure.safeDiagnostic()
                        : exception.getClass().getSimpleName();
                logger.warn("Review analysis batch failed; affected reviews marked ANALYSIS_FAILED "
                                + "(batchSize={}, cause={})",
                        reviewBatch.size(), diagnostic);
                // Leave the failed positions null and continue with later batches.
            }
        }

        List<ReviewAnalysisEntity> entitiesToSave = new ArrayList<>();
        for (int i = 0; i < reviews.size(); i++) {
            Review inputReview = reviews.get(i);
            Review persistedReview = managedReviews.get(inputReview.getReviewId());
            Review review = persistedReview == null ? inputReview : persistedReview;
            ReviewAnalysis analysis = results.get(i);
            review.setAnalysisStatus(analysis == null
                    ? AnalysisStatus.ANALYSIS_FAILED : AnalysisStatus.COMPLETED);
            inputReview.setAnalysisStatus(review.getAnalysisStatus());
            ReviewAnalysisEntity entity = existingByReview.get(inputReview.getReviewId());
            if (analysis == null) continue;
            if (entity == null) entity = new ReviewAnalysisEntity();
            entity.setReview(review);
            entity.setContentHash(keysByPosition.get(i));
            entity.setSentiment(analysis.getSentiment());
            entity.setTopics(new ArrayList<>(analysis.getTopics()));
            entity.setSummary(analysis.getSummary());
            entity.setNeedsReply(analysis.isNeeds_reply());
            entitiesToSave.add(entity);
        }
        reviewRepository.saveAll(new ArrayList<>(managedReviews.values()));
        analysisRepository.saveAll(entitiesToSave);
        return results;
    }

    private boolean matchesContent(ReviewAnalysisEntity entity, Review review, String expectedHash) {
        String storedHash = entity.getContentHash();
        // A legacy row without a hash cannot be safely matched after upload has
        // updated its associated Review row, so it must be analyzed once again.
        return storedHash != null && storedHash.equals(expectedHash);
    }

    private String contentHash(Review review) {
        String canonical = normalize(review.getProductName()) + "\u001f"
                + (review.getRating() == null ? "" : review.getRating()) + "\u001f"
                + normalize(review.getReviewText());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is not available.", impossible);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ")
                .strip().toLowerCase(Locale.ROOT);
    }

    private ReviewAnalysis convertToDto(ReviewAnalysisEntity entity) {
        return new ReviewAnalysis(entity.getSentiment(),
                entity.getTopics() == null ? new ArrayList<>()
                        : new ArrayList<>(entity.getTopics()),
                entity.getSummary(), entity.isNeedsReply());
    }

    private ReviewAnalysis copyAnalysis(ReviewAnalysis source) {
        return new ReviewAnalysis(source.getSentiment(),
                source.getTopics() == null ? new ArrayList<>()
                        : new ArrayList<>(source.getTopics()),
                source.getSummary(), source.isNeeds_reply());
    }
}
