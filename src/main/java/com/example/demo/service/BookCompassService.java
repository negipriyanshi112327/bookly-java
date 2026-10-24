package com.example.demo.service;

import com.example.demo.dto.CompassResult;
import com.example.demo.dto.RatingSummary;
import com.example.demo.dto.Recommendation;
import com.example.demo.model.Book;
import com.example.demo.repository.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * Book Compass: a transparent, rule-based recommender (no AI / machine learning).
 *
 * Points per book:
 *   favourite category matches  +3
 *   favourite author matches    +4
 *   each interest keyword found in title, description or category  +2
 *   rated 4 stars or more by readers  +1 (only as a bonus on top of a real match)
 * Books above the budget or out of stock are skipped. Highest score first, top 6.
 */
@Service
public class BookCompassService {

    public static final int MAX_RESULTS = 6;

    private final BookRepository books;
    private final ReviewService reviewService;

    public BookCompassService(BookRepository books, ReviewService reviewService) {
        this.books = books;
        this.reviewService = reviewService;
    }

    @Transactional(readOnly = true)
    public CompassResult recommend(String category, String author, String interests, BigDecimal maxPrice) {
        String wantedCategory = BookService.blankToNull(category);
        String wantedAuthor = BookService.blankToNull(author);
        List<String> keywords = keywords(interests);
        Map<Long, RatingSummary> ratings = reviewService.summaries();

        List<Recommendation> matches = new ArrayList<>();
        for (Book book : books.findByActiveTrue()) {
            if (!book.isInStock()) continue;
            if (maxPrice != null && book.getPrice().compareTo(maxPrice) > 0) continue;

            int score = 0;
            List<String> reasons = new ArrayList<>();

            if (wantedCategory != null && wantedCategory.equalsIgnoreCase(book.getCategory())) {
                score += 3;
                reasons.add("Matches your favourite category: " + book.getCategory());
            }
            if (wantedAuthor != null && book.getAuthor() != null
                    && book.getAuthor().toLowerCase(Locale.ROOT).contains(wantedAuthor.toLowerCase(Locale.ROOT))) {
                score += 4;
                reasons.add("Written by " + book.getAuthor());
            }
            String haystack = (safe(book.getName()) + " " + safe(book.getDescription()) + " "
                    + safe(book.getCategory())).toLowerCase(Locale.ROOT);
            for (String keyword : keywords) {
                if (haystack.contains(keyword)) {
                    score += 2;
                    reasons.add("Mentions \"" + keyword + "\"");
                }
            }
            if (score == 0) continue;

            RatingSummary rating = ratings.getOrDefault(book.getId(), RatingSummary.NONE);
            if (rating.getCount() > 0 && rating.getAverage() >= 4.0) {
                score += 1;
                reasons.add("Readers rate it " + rating.getDisplay() + "★");
            }
            if (maxPrice != null) {
                reasons.add("Within your budget of ₹" + maxPrice.stripTrailingZeros().toPlainString());
            }
            matches.add(new Recommendation(book, score, reasons, rating));
        }

        if (!matches.isEmpty()) {
            matches.sort(Comparator.comparingInt(Recommendation::getScore).reversed()
                    .thenComparing(r -> r.getBook().getName(), String.CASE_INSENSITIVE_ORDER));
            return new CompassResult(matches.subList(0, Math.min(MAX_RESULTS, matches.size())), false);
        }
        return new CompassResult(fallback(ratings, maxPrice), true);
    }

    /** Nothing matched: suggest well-rated, then featured, in-stock books (clearly labelled as a fallback). */
    private List<Recommendation> fallback(Map<Long, RatingSummary> ratings, BigDecimal maxPrice) {
        List<Recommendation> picks = new ArrayList<>();
        for (Book book : books.findByActiveTrue()) {
            if (!book.isInStock()) continue;
            if (maxPrice != null && book.getPrice().compareTo(maxPrice) > 0) continue;
            RatingSummary rating = ratings.getOrDefault(book.getId(), RatingSummary.NONE);
            List<String> reasons = new ArrayList<>();
            int score = 0;
            if (rating.getCount() > 0) {
                score += (int) Math.round(rating.getAverage());
                reasons.add("Popular with readers (" + rating.getDisplay() + "★)");
            }
            if (book.isFeatured()) {
                score += 2;
                reasons.add("Staff pick");
            }
            if (reasons.isEmpty()) reasons.add("In stock and ready to ship");
            picks.add(new Recommendation(book, score, reasons, rating));
        }
        picks.sort(Comparator.comparingInt(Recommendation::getScore).reversed()
                .thenComparing(r -> r.getBook().getName(), String.CASE_INSENSITIVE_ORDER));
        return picks.subList(0, Math.min(4, picks.size()));
    }

    /** Splits "habits, money; history" into distinct lowercase words of 3+ letters. */
    static List<String> keywords(String interests) {
        if (interests == null || interests.isBlank()) return List.of();
        Set<String> words = new LinkedHashSet<>();
        for (String part : interests.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (part.length() >= 3) words.add(part);
            if (words.size() == 5) break;
        }
        return new ArrayList<>(words);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
