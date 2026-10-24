package com.example.demo.service;

import com.example.demo.dto.CheckoutForm;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.InsufficientStockException;
import com.example.demo.exception.InvalidStatusTransitionException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.model.*;
import com.example.demo.repository.BookRepository;
import com.example.demo.repository.CustomerOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final CustomerOrderRepository orders;
    private final BookRepository books;
    private final AuthService authService;

    public OrderService(CustomerOrderRepository orders, BookRepository books, AuthService authService) {
        this.orders = orders;
        this.books = books;
        this.authService = authService;
    }

    /**
     * Places a Cash on Delivery order in ONE transaction:
     *  1. re-reads every book and its current price from the database,
     *  2. reserves stock with an atomic "stock = stock - qty WHERE stock >= qty" update,
     *  3. saves the order and its items with price snapshots.
     * If any step fails, everything is rolled back and the caller keeps the cart.
     * The checkout token makes a repeated submit return the first order instead of a new one.
     */
    @Transactional
    public CustomerOrder placeOrder(Long userId, CheckoutForm form, Map<Long, Integer> cartItems, String checkoutToken) {
        if (checkoutToken != null && !checkoutToken.isBlank()) {
            Optional<CustomerOrder> existing = orders.findByCheckoutToken(checkoutToken);
            if (existing.isPresent()) {
                CustomerOrder order = existing.get();
                if (order.getUser() != null && order.getUser().getId().equals(userId)) {
                    return order;
                }
                throw new BusinessException("This checkout form has expired. Please review your cart again.");
            }
        }
        if (cartItems == null || cartItems.isEmpty()) {
            throw new BusinessException("Your cart is empty.");
        }
        if (form.getPaymentMethod() != PaymentMethod.COD) {
            throw new BusinessException("Only Cash on Delivery is available.");
        }

        AppUser customer = authService.requireActiveCustomer(userId);

        // lock rows in a fixed order (by id) so two checkouts cannot deadlock each other
        List<Long> bookIds = new ArrayList<>(cartItems.keySet());
        Collections.sort(bookIds);
        Map<Long, Book> found = books.findAllById(bookIds).stream()
                .collect(Collectors.toMap(Book::getId, Function.identity()));

        CustomerOrder order = new CustomerOrder();
        order.setUser(customer);
        order.setCustomerName(form.getCustomerName().trim());
        order.setPhone(form.getPhone().replaceAll("[\\s-]", ""));
        order.setAddress(form.getAddress().trim());
        order.setPaymentMethod(PaymentMethod.COD);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setStatus(OrderStatus.PENDING);
        order.setOrderDate(LocalDateTime.now());
        order.setCheckoutToken(checkoutToken);

        BigDecimal total = BigDecimal.ZERO;
        for (Long bookId : bookIds) {
            int qty = cartItems.get(bookId) == null ? 0 : cartItems.get(bookId);
            Book book = found.get(bookId);
            if (book == null || !book.isActive()) {
                throw new BusinessException("A book in your cart is no longer available. Please review your cart.");
            }
            if (qty < 1 || qty > CartService.MAX_PER_BOOK) {
                throw new BusinessException("Invalid quantity for \"" + book.getName() + "\".");
            }
            if (books.decrementStock(bookId, qty) == 0) {
                int left = books.findStockById(bookId).orElse(0);
                throw new InsufficientStockException(left <= 0
                        ? "Sorry, \"" + book.getName() + "\" just sold out. Please remove it from your cart."
                        : "Only " + left + " cop" + (left == 1 ? "y" : "ies") + " of \"" + book.getName()
                          + "\" left. Please reduce the quantity in your cart.");
            }
            OrderItem item = new OrderItem(book, qty);
            order.addItem(item);
            total = total.add(item.getSubtotal());
        }
        order.setTotalAmount(BookService.money(total));
        return orders.saveAndFlush(order);
    }

    /** Finds an earlier order placed with this checkout token by this customer. */
    @Transactional(readOnly = true)
    public Optional<CustomerOrder> findByCheckoutToken(String token, Long userId) {
        if (token == null || token.isBlank()) return Optional.empty();
        return orders.findByCheckoutToken(token)
                .filter(o -> o.getUser() != null && o.getUser().getId().equals(userId));
    }

    @Transactional(readOnly = true)
    public List<CustomerOrder> ordersForCustomer(Long userId) {
        return orders.findByUserIdOrderByOrderDateDesc(userId);
    }

    /** A customer can only see their own orders; anything else looks like "not found". */
    @Transactional(readOnly = true)
    public CustomerOrder getOrderForCustomer(Long userId, Long orderId) {
        CustomerOrder order = orders.findWithItemsById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found."));
        if (order.getUser() == null || !order.getUser().getId().equals(userId)) {
            throw new NotFoundException("Order not found.");
        }
        return order;
    }

    @Transactional
    public CustomerOrder cancelByCustomer(Long userId, Long orderId) {
        CustomerOrder order = getOrderForCustomer(userId, orderId);
        if (!order.getStatus().isCancellableByCustomer()) {
            throw new InvalidStatusTransitionException("This order is " + order.getStatus().getLabel().toLowerCase()
                    + " and can no longer be cancelled.");
        }
        applyStatus(order, OrderStatus.CANCELLED);
        return order;
    }

    // ---------- admin ----------

    @Transactional(readOnly = true)
    public List<CustomerOrder> adminSearch(String query, OrderStatus status) {
        String q = BookService.blankToNull(query);
        Long orderId = null;
        String text = null;
        if (q != null) {
            String digits = q.startsWith("#") ? q.substring(1) : q;
            if (digits.matches("\\d{1,18}")) {
                orderId = Long.parseLong(digits);
            } else {
                text = q;
            }
        }
        return orders.adminSearch(status, orderId, text);
    }

    @Transactional(readOnly = true)
    public CustomerOrder getOrderForAdmin(Long orderId) {
        return orders.findWithItemsById(orderId).orElseThrow(() -> new NotFoundException("Order not found."));
    }

    @Transactional
    public CustomerOrder updateStatusByAdmin(Long orderId, OrderStatus newStatus) {
        CustomerOrder order = getOrderForAdmin(orderId);
        applyStatus(order, newStatus);
        return order;
    }

    /** The single place where status changes happen, so the rules and stock handling are always the same. */
    private void applyStatus(CustomerOrder order, OrderStatus next) {
        OrderStatus current = order.getStatus();
        if (current == null || !current.canTransitionTo(next)) {
            throw new InvalidStatusTransitionException(current == null ? OrderStatus.PENDING : current, next);
        }
        if (next == OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                if (item.getBookId() != null) {
                    books.incrementStock(item.getBookId(), item.getQuantity());
                }
            }
            order.setPaymentStatus(PaymentStatus.CANCELLED);
        } else if (next == OrderStatus.DELIVERED) {
            order.setPaymentStatus(PaymentStatus.PAID); // cash collected on delivery
        }
        order.setStatus(next);
    }
}
