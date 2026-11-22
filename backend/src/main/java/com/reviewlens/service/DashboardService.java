package com.reviewlens.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.reviewlens.entity.ReviewAnalysisEntity;
import com.reviewlens.entity.AnalysisStatus;
import com.reviewlens.repository.ReviewAnalysisRepository;
import com.reviewlens.repository.ReviewRepository;

@Service
public class DashboardService {

    private static final Set<String> SENTIMENTS =
            Set.of("positive", "neutral", "negative");
    private static final Set<String> TOPICS = Set.of(
            "quality", "delivery", "price", "packaging", "support",
            "size_fit", "other");

    private final ReviewRepository reviewRepository;
    private final ReviewAnalysisRepository analysisRepository;

    public DashboardService(ReviewRepository reviewRepository,
                            ReviewAnalysisRepository analysisRepository) {
        this.reviewRepository = reviewRepository;
        this.analysisRepository = analysisRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStatistics() {
        return getDashboardStatistics(null, null, null);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStatistics(
            String product, String sentiment, String topic) {
        String productFilter = normalize(product);
        String sentimentFilter = normalize(sentiment);
        String topicFilter = normalize(topic);
        if (!sentimentFilter.isEmpty() && !SENTIMENTS.contains(sentimentFilter)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported sentiment filter.");
        }
        if (!topicFilter.isEmpty() && !TOPICS.contains(topicFilter)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported topic filter.");
        }

        List<ReviewAnalysisEntity> analyses =
                analysisRepository.findFilteredForDashboard(
                        productFilter, sentimentFilter, topicFilter);
        long totalReviews = reviewRepository.countFiltered(
                productFilter, sentimentFilter, topicFilter);
        long positive = 0;
        long negative = 0;
        long neutral = 0;
        long needsReply = 0;
        Map<String, Long> complaintTopics = new HashMap<>();
        Map<String, Long> praiseTopics = new HashMap<>();

        for (ReviewAnalysisEntity analysis : analyses) {
            if (analysis.getReview().getAnalysisStatus() == AnalysisStatus.ANALYSIS_FAILED) {
                continue;
            }
            String value = analysis.getSentiment().toLowerCase(Locale.ROOT);
            switch (value) {
                case "positive" -> positive++;
                case "negative" -> negative++;
                case "neutral" -> neutral++;
                default -> { }
            }
            if (analysis.isNeedsReply()) needsReply++;
            if (analysis.getTopics() == null) continue;
            Map<String, Long> target = value.equals("negative")
                    ? complaintTopics : value.equals("positive") ? praiseTopics : null;
            if (target != null) {
                for (String topicValue : new HashSet<>(analysis.getTopics())) {
                    target.merge(topicValue, 1L, Long::sum);
                }
            }
        }

        long analyzed = analyses.size();
        Map<String, Long> counts = Map.of(
                "positive", positive, "negative", negative, "neutral", neutral);
        Map<String, Double> percentages = new LinkedHashMap<>();
        percentages.put("positive", percentage(positive, analyzed));
        percentages.put("negative", percentage(negative, analyzed));
        percentages.put("neutral", percentage(neutral, analyzed));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalReviews", totalReviews);
        result.put("analyzedReviews", analyzed);
        result.put("totalAnalyzedReviews", analyzed);
        result.put("sentimentCounts", counts);
        result.put("sentimentPercentages", percentages);
        result.put("positive", positive);
        result.put("negative", negative);
        result.put("neutral", neutral);
        result.put("needsReply", needsReply);
        result.put("reviewsNeedingReply", needsReply);
        result.put("topComplaints", getTopThree(complaintTopics));
        result.put("topPraises", getTopThree(praiseTopics));
        result.put("filters", Map.of(
                "product", productFilter,
                "sentiment", sentimentFilter,
                "topic", topicFilter));
        return result;
    }

    private String normalize(String value) {
        return value == null || value.isBlank()
                ? "" : value.strip().toLowerCase(Locale.ROOT);
    }

    private double percentage(long count, long total) {
        if (total == 0) return 0.0;
        return Math.round((count * 1000.0) / total) / 10.0;
    }

    private List<Map<String, Object>> getTopThree(Map<String, Long> topicCounts) {
        return topicCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(3)
                .map(entry -> Map.<String, Object>of(
                        "topic", entry.getKey(), "count", entry.getValue()))
                .collect(Collectors.toList());
    }
}
