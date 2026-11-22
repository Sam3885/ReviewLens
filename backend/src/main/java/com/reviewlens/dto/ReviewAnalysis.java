
package com.reviewlens.dto;

import java.util.List;

public class ReviewAnalysis {

    private String sentiment;
    private List<String> topics;
    private String summary;
    private boolean needs_reply;

    public ReviewAnalysis() {
    }

    public ReviewAnalysis(String sentiment, List<String> topics,
                          String summary, boolean needs_reply) {
        this.sentiment = sentiment;
        this.topics = topics;
        this.summary = summary;
        this.needs_reply = needs_reply;
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

    public boolean isNeeds_reply() {
        return needs_reply;
    }

    public void setNeeds_reply(boolean needs_reply) {
        this.needs_reply = needs_reply;
    }
}
