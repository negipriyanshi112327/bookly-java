package com.example.demo.service;

import com.example.demo.dto.RatingSummary;
import com.example.demo.dto.ReviewForm;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.model.AppUser;
import com.example.demo.model.Book;
import com.example.demo.model.OrderStatus;
import com.example.demo.model.Review;
import com.example.demo.repository.BookRepository;
import com.example.demo.repository.CustomerOrderRepository;
import com.example.demo.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reviews are limited to verified purchasers: the customer must have a DELIVERED order
 * that contains the book. One review per customer per book; submitting again edits it.
 */
@Service
public class ReviewService {

    private final ReviewRepository reviews;
    private final BookRepository books;
    private final CustomerOrderRepository orders;
    private final AuthService authService;

    public ReviewService(ReviewRepository reviews, BookRepository books,
                         CustomerOrderRepository orders, AuthService authService) {
        this.reviews = reviews;
        this.books = books;
        this.orders = orders;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public List<Review> forBook(Long bookId) {
        return reviews.findByBookIdOrderByCreatedAtDesc(bookId);
    }

    @Transactional(readOnly = true)
    public RatingSummary summary(Long bookId) {
        List<Object[]> rows = reviews.ratingSummaryForBook(bookId);
        if (rows.isEmpty() || rows.get(0)[0] == null) return RatingSummary.NONE;
        Object[] row = rows.get(0);
        return new RatingSummary(((Number) row[0]).doubleValue(), ((Number) row[1]).longValue());
    }

    /** bookId -> rating summary for every reviewed book (used on catalogue cards). */
    @Transactional(readOnly = true)
    public Map<Long, RatingSummary> summaries() {
        Map<Long, RatingSummary> result = new HashMap<>();
        for (Object[] row : reviews.ratingSummaries()) {
            result.put(((Number) row[0]).longValue(),
                    new RatingSummary(((Number) row[1]).doubleValue(), ((Number) row[2]).longValue()));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public boolean hasPurchased(Long userId, Long bookId) {
        return orders.countPurchases(userId, bookId, OrderStatus.DELIVERED) > 0;
    }

    @Transactional(readOnly = true)
    public Optional<Review> findOwn(Long userId, Long bookId) {
        return reviews.findByUserIdAndBookId(userId, bookId);
    }

    /** Creates the customer's review, or updates it if they already reviewed this book. */
    @Transactional
    public Review save(Long userId, Long bookId, ReviewForm form) {
        if (form.getRating() == null || form.getRating() < 1 || form.getRating() > 5) {
            throw new BusinessException("Rating must be between 1 and 5 stars.");
        }
        String comment = form.getComment() == null ? "" : form.getComment().trim();
        if (comment.length() < 10 || comment.length() > 1000) {
            throw new BusinessException("Review must be between 10 and 1000 characters.");
        }
        Book book = books.findById(bookId).orElseThrow(() -> new NotFoundException("Book not found."));
        if (!hasPurchased(userId, bookId)) {
            throw new BusinessException("Only customers who have received this book can review it.");
        }
        AppUser user = authService.requireActiveCustomer(userId);

        Review review = reviews.findByUserIdAndBookId(userId, bookId).orElseGet(() -> new Review(user, book));
        review.setRating(form.getRating());
        review.setComment(comment);
        return reviews.save(review);
    }

    /** Customers can delete only their own review. */
    @Transactional
    public void deleteOwn(Long userId, Long bookId) {
        Review review = reviews.findByUserIdAndBookId(userId, bookId)
                .orElseThrow(() -> new NotFoundException("You have not reviewed this book."));
        reviews.delete(review);
    }
}
