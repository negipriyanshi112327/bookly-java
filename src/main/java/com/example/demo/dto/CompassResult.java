package com.example.demo.dto;

import java.util.List;

/** fallback = true means nothing matched the preferences, so popular in-stock books are shown instead. */
public final class CompassResult {

    private final List<Recommendation> recommendations;
    private final boolean fallback;

    public CompassResult(List<Recommendation> recommendations, boolean fallback) {
        this.recommendations = recommendations;
        this.fallback = fallback;
    }

    public List<Recommendation> getRecommendations() { return recommendations; }
    public boolean isFallback() { return fallback; }

}
