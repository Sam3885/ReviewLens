package com.reviewlens.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.reviewlens.dto.ReviewAnalysis;
import com.reviewlens.entity.AnalysisStatus;
import com.reviewlens.entity.Review;
import com.reviewlens.entity.ReviewAnalysisEntity;
import com.reviewlens.repository.ReviewAnalysisRepository;
import com.reviewlens.repository.ReviewRepository;

class ReviewAnalysisServiceTests {

    @Test
    void reusesPersistedContentAnalysisForDifferentReviewId() {
        GeminiService gemini = mock(GeminiService.class);
        ReviewAnalysisRepository analyses = mock(ReviewAnalysisRepository.class);
        ReviewRepository reviews = mock(ReviewRepository.class);
        Review existingReview = new Review("original", "Desk Lamp", 2,
                "The switch is broken", null);
        ReviewAnalysisEntity persisted = new ReviewAnalysisEntity(existingReview,
                "negative", List.of("quality", "support"), "Switch does not work.", true);
        Review incoming = new Review("imported", "Desk Lamp", 2,
                "The switch is broken", null);
        when(reviews.findAllById(List.of("imported"))).thenReturn(List.of(incoming));
        when(analyses.findByReview_ReviewIdIn(List.of("imported")))
                .thenReturn(List.of());
        when(analyses.findByContentHashIn(anyCollection())).thenAnswer(invocation -> {
            Collection<String> hashes = invocation.getArgument(0);
            persisted.setContentHash(hashes.iterator().next());
            return List.of(persisted);
        });
        when(reviews.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(analyses.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewAnalysisService service = new ReviewAnalysisService(gemini, analyses, reviews);
        List<ReviewAnalysis> result = service.analyzeReviews(List.of(incoming));

        assertEquals("negative", result.get(0).getSentiment());
        assertEquals(AnalysisStatus.COMPLETED, incoming.getAnalysisStatus());
        verify(gemini, never()).analyzeBatch(anyList());
        verify(analyses).saveAll(any());
    }

    @Test
    void processesFiveAtATimeAndMarksOnlyFailedBatchReviews() {
        GeminiService gemini = mock(GeminiService.class);
        ReviewAnalysisRepository analyses = mock(ReviewAnalysisRepository.class);
        ReviewRepository reviews = mock(ReviewRepository.class);
        List<Review> input = new ArrayList<>();
        List<Integer> batchSizes = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            input.add(new Review("r" + i, "Product", 4, "Review " + i, null));
        }
        when(reviews.findAllById(anyCollection())).thenReturn(input);
        when(analyses.findByReview_ReviewIdIn(anyCollection())).thenReturn(List.of());
        when(analyses.findByContentHashIn(anyCollection())).thenReturn(List.of());
        when(gemini.analyzeBatch(anyList())).thenAnswer(invocation -> {
                    List<Review> batch = invocation.getArgument(0);
                    batchSizes.add(batch.size());
                    if (batchSizes.size() == 1) {
                        throw new IllegalStateException("simulated provider failure");
                    }
                    return batch.stream().map(ignored -> new ReviewAnalysis(
                            "positive", List.of("quality"), "Good", false)).toList();
                });
        when(reviews.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(analyses.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewAnalysisService service = new ReviewAnalysisService(gemini, analyses, reviews);
        List<ReviewAnalysis> result = service.analyzeReviews(input);

        assertEquals(6, result.size());
        for (int i = 0; i < 5; i++) {
            assertNull(result.get(i));
            assertEquals(AnalysisStatus.ANALYSIS_FAILED,
                    input.get(i).getAnalysisStatus());
        }
        assertEquals("positive", result.get(5).getSentiment());
        assertEquals(AnalysisStatus.COMPLETED, input.get(5).getAnalysisStatus());
        assertEquals(List.of(5, 1), batchSizes);
        verify(gemini, org.mockito.Mockito.times(2)).analyzeBatch(anyList());
    }
}
