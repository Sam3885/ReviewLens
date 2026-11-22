package com.reviewlens.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import com.reviewlens.dto.SuggestedReplyResponse;
import com.reviewlens.entity.Review;
import com.reviewlens.entity.ReviewAnalysisEntity;
import com.reviewlens.repository.ReviewAnalysisRepository;
import com.reviewlens.repository.ReviewRepository;

class ReviewApiServiceTests {

    @Test
    void listsReviewsWithFiltersAndPagination() {
        ReviewRepository reviews = mock(ReviewRepository.class);
        ReviewAnalysisRepository analyses = mock(ReviewAnalysisRepository.class);
        GeminiService gemini = mock(GeminiService.class);
        Review review = new Review("r1", "Camera", 2, "Needs work", null);
        when(reviews.findFiltered(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(review), PageRequest.of(0, 10), 1));
        when(analyses.findByReview_ReviewIdIn(List.of("r1"))).thenReturn(List.of());

        var result = new ReviewApiService(reviews, analyses, gemini)
                .listReviews(0, 10, " Camera ", "NEGATIVE", "quality");

        assertEquals(1, result.getTotalElements());
        assertEquals("r1", result.getContent().get(0).reviewId());
        verify(reviews).findFiltered(org.mockito.ArgumentMatchers.eq("camera"),
                org.mockito.ArgumentMatchers.eq("negative"),
                org.mockito.ArgumentMatchers.eq("quality"), any());
    }

    @Test
    void listsReviewsWithOmittedAndBlankFiltersAsNonNullEmptyStrings() {
        ReviewRepository reviews = mock(ReviewRepository.class);
        ReviewAnalysisRepository analyses = mock(ReviewAnalysisRepository.class);
        GeminiService gemini = mock(GeminiService.class);
        when(reviews.findFiltered(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        new ReviewApiService(reviews, analyses, gemini)
                .listReviews(0, 20, null, " ", "");

        verify(reviews).findFiltered(org.mockito.ArgumentMatchers.eq(""),
                org.mockito.ArgumentMatchers.eq(""),
                org.mockito.ArgumentMatchers.eq(""), any());
    }

    @Test
    void generatesReplyOnlyWhenAnalysisRequiresOne() {
        ReviewRepository reviews = mock(ReviewRepository.class);
        ReviewAnalysisRepository analyses = mock(ReviewAnalysisRepository.class);
        GeminiService gemini = mock(GeminiService.class);
        Review review = new Review("r1", "Camera", 1, "Broken", null);
        ReviewAnalysisEntity analysis = new ReviewAnalysisEntity(review,
                "negative", List.of("quality"), "Broken item.", true);
        when(reviews.findById("r1")).thenReturn(Optional.of(review));
        when(analyses.findByReview_ReviewId("r1")).thenReturn(Optional.of(analysis));
        when(gemini.generateSuggestedReply(review)).thenReturn("Sorry about this issue.");

        SuggestedReplyResponse response = new ReviewApiService(reviews, analyses, gemini)
                .suggestReply("r1");

        assertEquals("Sorry about this issue.", response.suggestedReply());
        verify(gemini).generateSuggestedReply(review);
    }

    @Test
    void refusesReplyWhenReviewIsNotMarkedForReply() {
        ReviewRepository reviews = mock(ReviewRepository.class);
        ReviewAnalysisRepository analyses = mock(ReviewAnalysisRepository.class);
        GeminiService gemini = mock(GeminiService.class);
        Review review = new Review("r1", "Camera", 5, "Excellent", null);
        when(reviews.findById("r1")).thenReturn(Optional.of(review));
        when(analyses.findByReview_ReviewId("r1")).thenReturn(Optional.of(
                new ReviewAnalysisEntity(review, "positive", List.of("quality"), "Good.", false)));

        assertThrows(ResponseStatusException.class,
                () -> new ReviewApiService(reviews, analyses, gemini).suggestReply("r1"));
        verify(gemini, never()).generateSuggestedReply(any());
    }
}
