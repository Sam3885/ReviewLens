
package com.reviewlens.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "review_analyses")
public class ReviewAnalysisEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false, unique = true)
    private Review review;

    @Column(nullable = false, length = 20)
    private String sentiment;

    @ElementCollection
    @CollectionTable(
        name = "review_analysis_topics",
        joinColumns = @JoinColumn(name = "analysis_id")
    )
    @OrderColumn(name = "topic_order")
    @Column(name = "topic", nullable = false)
    private List<String> topics = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "needs_reply", nullable = false)
    private boolean needsReply;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    public ReviewAnalysisEntity() {
    }

    public ReviewAnalysisEntity(
            Review review,
            String sentiment,
            List<String> topics,
            String summary,
            boolean needsReply) {

        this.review = review;
        this.sentiment = sentiment;
        this.topics = topics != null
                ? new ArrayList<>(topics)
                : new ArrayList<>();
        this.summary = summary;
        this.needsReply = needsReply;
    }

    public Long getId() {
        return id;
    }

    public Review getReview() {
        return review;
    }

    public void setReview(Review review) {
        this.review = review;
    }

    public String getSentiment() {
        return sentiment;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    public List<String> getTopics() {
        return topics;
    }

    public void setTopics(List<String> topics) {
        this.topics = topics;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public boolean isNeedsReply() {
        return needsReply;
    }

    public void setNeedsReply(boolean needsReply) {
        this.needsReply = needsReply;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }
}
