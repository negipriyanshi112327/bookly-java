package com.example.demo.service;

import com.example.demo.dto.CartLine;
import com.example.demo.dto.CartView;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.model.Book;
import com.example.demo.repository.BookRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.WebUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Session-based cart. The session stores only bookId -> quantity;
 * prices and totals are always recalculated from the database.
 */
@Service
public class CartService {

    public static final String SESSION_KEY = "cart";
    public static final int MAX_PER_BOOK = 10;

    private final BookRepository books;

    public CartService(BookRepository books) {
        this.books = books;
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Integer> cart(HttpSession session) {
        Object value = session.getAttribute(SESSION_KEY);
        if (value instanceof Map<?, ?> map) {
            return (Map<Long, Integer>) map;
        }
        Map<Long, Integer> cart = new LinkedHashMap<>();
        session.setAttribute(SESSION_KEY, cart);
        return cart;
    }

    /** Re-set the attribute so the change is saved even with session replication. */
    private void save(HttpSession session, Map<Long, Integer> cart) {
        session.setAttribute(SESSION_KEY, cart);
    }

    public static int maxAllowed(Book book) {
        return Math.min(book.getStockQuantity(), MAX_PER_BOOK);
    }

    /** Adds copies of a book. Returns a message describing what happened. */
    @Transactional(readOnly = true)
    public String add(HttpSession session, Long bookId, int quantity) {
        if (quantity < 1) {
            throw new BusinessException("Quantity must be at least 1.");
        }
        Book book = books.findByIdAndActiveTrue(bookId)
                .orElseThrow(() -> new NotFoundException("That book is not available."));
        if (book.getStockQuantity() <= 0) {
            throw new BusinessException("Sorry, \"" + book.getName() + "\" is out of stock.");
        }

        synchronized (WebUtils.getSessionMutex(session)) {
            Map<Long, Integer> cart = cart(session);
            int current = cart.getOrDefault(bookId, 0);
            int limit = maxAllowed(book);
            if (current >= limit) {
                throw new BusinessException("You already have the maximum of " + limit
                        + " cop" + (limit == 1 ? "y" : "ies") + " of \"" + book.getName() + "\" in your cart.");
            }
            int wanted = current + quantity;
            int finalQty = Math.min(wanted, limit);
            cart.put(bookId, finalQty);
            save(session, cart);
            if (finalQty < wanted) {
                return "Only " + limit + " copies of \"" + book.getName() + "\" can be ordered, so your cart has " + finalQty + ".";
            }
            return "\"" + book.getName() + "\" was added to your cart.";
        }
    }

    /** Sets the quantity of a line (from the + / − buttons or the number box). */
    @Transactional(readOnly = true)
    public void update(HttpSession session, Long bookId, int quantity) {
        synchronized (WebUtils.getSessionMutex(session)) {
            Map<Long, Integer> cart = cart(session);
            if (!cart.containsKey(bookId)) {
                throw new BusinessException("That book is not in your cart.");
            }
            if (quantity < 1) {
                throw new BusinessException("Quantity must be at least 1. Use Remove to take a book out of your cart.");
            }
            Optional<Book> found = books.findByIdAndActiveTrue(bookId);
            if (found.isEmpty()) {
                cart.remove(bookId);
                save(session, cart);
                throw new BusinessException("That book is no longer available and was removed from your cart.");
            }
            Book book = found.get();
            int limit = maxAllowed(book);
            if (limit <= 0) {
                throw new BusinessException("\"" + book.getName() + "\" is out of stock. Please remove it to continue.");
            }
            if (quantity > limit) {
                throw new BusinessException("Only " + limit + " cop" + (limit == 1 ? "y" : "ies")
                        + " of \"" + book.getName() + "\" can be ordered.");
            }
            cart.put(bookId, quantity);
            save(session, cart);
        }
    }

    public void remove(HttpSession session, Long bookId) {
        synchronized (WebUtils.getSessionMutex(session)) {
            Map<Long, Integer> cart = cart(session);
            cart.remove(bookId);
            save(session, cart);
        }
    }

    public void clear(HttpSession session) {
        session.removeAttribute(SESSION_KEY);
    }

    /** Copy of the raw cart (bookId -> quantity) for placing an order. */
    public Map<Long, Integer> snapshot(HttpSession session) {
        synchronized (WebUtils.getSessionMutex(session)) {
            return new LinkedHashMap<>(cart(session));
        }
    }

    /** Number of copies in the cart, for the navbar badge. Does not create a session. */
    public static int itemCount(HttpSession session) {
        if (session == null) return 0;
        Object value = session.getAttribute(SESSION_KEY);
        if (value instanceof Map<?, ?> map) {
            int count = 0;
            for (Object qty : map.values()) {
                if (qty instanceof Integer i) count += i;
            }
            return count;
        }
        return 0;
    }

    /**
     * Builds the cart page from current database data. Books that were deleted or archived
     * are removed; quantities above the available stock are reduced; out-of-stock lines
     * are flagged so checkout is blocked until they are removed.
     */
    @Transactional(readOnly = true)
    public CartView view(HttpSession session) {
        synchronized (WebUtils.getSessionMutex(session)) {
            Map<Long, Integer> cart = cart(session);
            Map<Long, Book> found = books.findAllById(cart.keySet()).stream()
                    .collect(Collectors.toMap(Book::getId, Function.identity()));

            List<CartLine> lines = new ArrayList<>();
            List<String> notices = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            int totalItems = 0;

            Iterator<Map.Entry<Long, Integer>> it = cart.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Long, Integer> entry = it.next();
                Book book = found.get(entry.getKey());
                if (book == null || !book.isActive()) {
                    it.remove();
                    notices.add("A book in your cart is no longer available and was removed.");
                    continue;
                }
                int qty = entry.getValue();
                int limit = maxAllowed(book);
                String issue = null;
                if (limit <= 0) {
                    issue = "Out of stock – please remove this book to check out.";
                } else if (qty > limit) {
                    qty = limit;
                    entry.setValue(qty);
                    notices.add("Only " + limit + " copies of \"" + book.getName()
                            + "\" are available, so the quantity was reduced.");
                }
                BigDecimal subtotal = book.getPrice().multiply(BigDecimal.valueOf(qty));
                lines.add(new CartLine(book, qty, book.getPrice(), subtotal, Math.max(limit, 0), issue));
                if (issue == null) {
                    total = total.add(subtotal);
                    totalItems += qty;
                }
            }
            save(session, cart);
            return new CartView(lines, BookService.money(total), totalItems, notices);
        }
    }
}
