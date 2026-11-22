package com.reviewlens.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.reviewlens.entity.Review;
import com.reviewlens.entity.ReviewAnalysisEntity;
import com.reviewlens.repository.ReviewAnalysisRepository;
import com.reviewlens.repository.ReviewRepository;

class DashboardServiceTests {

    @Test
    void aggregatesCountsPercentagesReplyNeedsAndTopTopicsAcrossProducts() {
        ReviewRepository reviews = mock(ReviewRepository.class);
        ReviewAnalysisRepository analyses = mock(ReviewAnalysisRepository.class);
        List<ReviewAnalysisEntity> rows = List.of(
                analysis("r1", "Product A", "negative", List.of("quality", "quality"), true),
                analysis("r2", "Product B", "negative", List.of("delivery"), false),
                analysis("r3", "Product B", "positive", List.of("packaging"), false),
                analysis("r4", "Product C", "positive", List.of("quality"), true),
                analysis("r5", "Product C", "neutral", List.of("other"), false));
        when(analyses.findFilteredForDashboard("", "", "")).thenReturn(rows);
        when(reviews.countFiltered("", "", "")).thenReturn(6L);

        Map<String, Object> stats = new DashboardService(reviews, analyses)
                .getDashboardStatistics();

        assertEquals(6L, stats.get("totalReviews"));
        assertEquals(5L, stats.get("totalAnalyzedReviews"));
        assertEquals(2L, stats.get("positive"));
        assertEquals(2L, stats.get("negative"));
        assertEquals(1L, stats.get("neutral"));
        assertEquals(2L, stats.get("reviewsNeedingReply"));
        assertEquals(Map.of("positive", 40.0, "negative", 40.0, "neutral", 20.0),
                stats.get("sentimentPercentages"));
        assertEquals(List.of(Map.of("topic", "packaging", "count", 1L),
                        Map.of("topic", "quality", "count", 1L)),
                stats.get("topPraises"));
        assertEquals(List.of(Map.of("topic", "delivery", "count", 1L),
                        Map.of("topic", "quality", "count", 1L)),
                stats.get("topComplaints"));
    }

    @Test
    void forwardsNormalizedFiltersToRepositories() {
        ReviewRepository reviews = mock(ReviewRepository.class);
        ReviewAnalysisRepository analyses = mock(ReviewAnalysisRepository.class);
        when(analyses.findFilteredForDashboard("camera", "negative", "delivery"))
                .thenReturn(List.of());
        when(reviews.countFiltered("camera", "negative", "delivery"))
                .thenReturn(0L);

        new DashboardService(reviews, analyses)
                .getDashboardStatistics(" Camera ", " NEGATIVE ", "Delivery");

        verify(analyses).findFilteredForDashboard("camera", "negative", "delivery");
        verify(reviews).countFiltered("camera", "negative", "delivery");
    }

    @Test
    void convertsMissingAndBlankFiltersToNonNullEmptyStrings() {
        ReviewRepository reviews = mock(ReviewRepository.class);
        ReviewAnalysisRepository analyses = mock(ReviewAnalysisRepository.class);
        when(analyses.findFilteredForDashboard("", "", "")).thenReturn(List.of());
        when(reviews.countFiltered("", "", "")).thenReturn(0L);

        Map<String, Object> stats = new DashboardService(reviews, analyses)
                .getDashboardStatistics(null, "  ", "");

        assertEquals(Map.of("product", "", "sentiment", "", "topic", ""),
                stats.get("filters"));
        verify(analyses).findFilteredForDashboard("", "", "");
        verify(reviews).countFiltered("", "", "");
    }

    private ReviewAnalysisEntity analysis(String id, String product,
                                          String sentiment, List<String> topics,
                                          boolean needsReply) {
        ReviewAnalysisEntity entity = new ReviewAnalysisEntity(
                new Review(id, product, 3, "Review", null),
                sentiment, topics, "Summary", needsReply);
        return entity;
    }
}
