package com.reviewlens.entity;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AnalysisStatus {
    PENDING,
    COMPLETED,
    ANALYSIS_FAILED;

    @JsonValue
    public String toJsonValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
