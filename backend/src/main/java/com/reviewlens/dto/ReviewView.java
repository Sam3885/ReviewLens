package com.reviewlens.dto;

import java.time.LocalDate;

import com.reviewlens.entity.AnalysisStatus;

public record ReviewView(
        String reviewId,
        String productName,
        Integer rating,
        String reviewText,
        LocalDate reviewDate,
        AnalysisStatus analysisStatus,
        ReviewAnalysis analysis) {
}
