package com.example.demo.service;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.model.AppUser;
import com.example.demo.model.Book;
import com.example.demo.model.WishlistItem;
import com.example.demo.repository.BookRepository;
import com.example.demo.repository.WishlistRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Wishlists are stored in MySQL, so they survive logout and work on any device. */
@Service
public class WishlistService {

    private final WishlistRepository wishlists;
    private final BookRepository books;
    private final AuthService authService;
    private final CartService cartService;

    public WishlistService(WishlistRepository wishlists, BookRepository books,
                           AuthService authService, CartService cartService) {
        this.wishlists = wishlists;
        this.books = books;
        this.authService = authService;
        this.cartService = cartService;
    }

    @Transactional(readOnly = true)
    public List<WishlistItem> list(Long userId) {
        return wishlists.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public Set<Long> bookIds(Long userId) {
        return new HashSet<>(wishlists.findBookIdsByUserId(userId));
    }

    @Transactional(readOnly = true)
    public boolean contains(Long userId, Long bookId) {
        return wishlists.existsByUserIdAndBookId(userId, bookId);
    }

    @Transactional
    public String add(Long userId, Long bookId) {
        Book book = books.findByIdAndActiveTrue(bookId)
                .orElseThrow(() -> new NotFoundException("That book is not available."));
        if (wishlists.existsByUserIdAndBookId(userId, bookId)) {
            throw new BusinessException("\"" + book.getName() + "\" is already in your wishlist.");
        }
        AppUser user = authService.requireActiveCustomer(userId);
        try {
            wishlists.saveAndFlush(new WishlistItem(user, book));
        } catch (DataIntegrityViolationException e) {
            // the unique (user_id, book_id) constraint caught a double click
            throw new BusinessException("\"" + book.getName() + "\" is already in your wishlist.");
        }
        return "\"" + book.getName() + "\" was saved to your wishlist.";
    }

    @Transactional
    public void remove(Long userId, Long bookId) {
        WishlistItem item = wishlists.findByUserIdAndBookId(userId, bookId)
                .orElseThrow(() -> new NotFoundException("That book is not in your wishlist."));
        wishlists.delete(item);
    }

    /** Adds the book to the cart, then removes it from the wishlist (only if adding worked). */
    @Transactional
    public String moveToCart(Long userId, Long bookId, HttpSession session) {
        WishlistItem item = wishlists.findByUserIdAndBookId(userId, bookId)
                .orElseThrow(() -> new NotFoundException("That book is not in your wishlist."));
        String message = cartService.add(session, bookId, 1);
        wishlists.delete(item);
        return message;
    }
}
