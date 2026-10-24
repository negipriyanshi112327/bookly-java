package com.example.demo.dto;

import com.example.demo.model.Book;

import java.util.List;

/** A Book Compass suggestion with its score and the plain-language reasons behind it. */
public final class Recommendation {

    private final Book book;
    private final int score;
    private final List<String> reasons;
    private final RatingSummary rating;

    public Recommendation(Book book, int score, List<String> reasons, RatingSummary rating) {
        this.book = book;
        this.score = score;
        this.reasons = reasons;
        this.rating = rating;
    }

    public Book getBook() { return book; }
    public int getScore() { return score; }
    public List<String> getReasons() { return reasons; }
    public RatingSummary getRating() { return rating; }

}
