package com.example.demo.dto;

public final class RatingSummary {

    private final double average;
    private final long count;

    public RatingSummary(double average, long count) {
        this.average = average;
        this.count = count;
    }

    public double getAverage() { return average; }
    public long getCount() { return count; }


    public static final RatingSummary NONE = new RatingSummary(0, 0);

    public boolean hasReviews() {
        return count > 0;
    }

    /** Average rounded to one decimal, e.g. "4.3". */
    public String getDisplay() {
        return String.format("%.1f", average);
    }

    /** Whole stars for the star row (rounded). */
    public int getStars() {
        return (int) Math.round(average);
    }
}
