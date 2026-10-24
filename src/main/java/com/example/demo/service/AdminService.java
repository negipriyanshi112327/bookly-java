package com.example.demo.service;

import com.example.demo.dto.CustomerSummary;
import com.example.demo.dto.DashboardStats;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.model.AppUser;
import com.example.demo.model.Book;
import com.example.demo.model.CustomerOrder;
import com.example.demo.model.OrderStatus;
import com.example.demo.model.Role;
import com.example.demo.repository.BookRepository;
import com.example.demo.repository.CustomerOrderRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private final BookRepository books;
    private final UserRepository users;
    private final CustomerOrderRepository orders;

    public AdminService(BookRepository books, UserRepository users, CustomerOrderRepository orders) {
        this.books = books;
        this.users = users;
        this.orders = orders;
    }

    @Transactional(readOnly = true)
    public DashboardStats stats() {
        return new DashboardStats(
                books.countByActiveTrue(),
                users.countByRole(Role.CUSTOMER),
                orders.count(),
                orders.countByStatus(OrderStatus.PENDING),
                books.totalUnitsInStock(),
                books.countByActiveTrueAndStockQuantityLessThanEqual(Book.LOW_STOCK_LIMIT),
                BookService.money(orders.sumTotalByStatus(OrderStatus.DELIVERED)));
    }

    @Transactional(readOnly = true)
    public List<CustomerOrder> recentOrders() {
        return orders.findTop5ByOrderByOrderDateDesc();
    }

    @Transactional(readOnly = true)
    public List<Book> lowStock() {
        return books.findByActiveTrueAndStockQuantityLessThanEqualOrderByStockQuantityAsc(Book.LOW_STOCK_LIMIT);
    }

    /** Customer list for the admin. Built as DTOs so password hashes never reach a page. */
    @Transactional(readOnly = true)
    public List<CustomerSummary> customers(String query) {
        Map<Long, Long> orderCounts = new HashMap<>();
        for (Object[] row : orders.countOrdersPerUser()) {
            orderCounts.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return users.searchByRole(Role.CUSTOMER, BookService.blankToNull(query)).stream()
                .map(u -> new CustomerSummary(u.getId(), u.getName(), u.getEmail(), u.isEnabled(),
                        u.getCreatedAt(), orderCounts.getOrDefault(u.getId(), 0L)))
                .toList();
    }

    /** Activates / deactivates a customer. A deactivated customer cannot log in or order. */
    @Transactional
    public AppUser setCustomerEnabled(Long userId, boolean enabled) {
        AppUser user = users.findById(userId).orElseThrow(() -> new NotFoundException("Customer not found."));
        if (user.getRole() != Role.CUSTOMER) {
            throw new BusinessException("Only customer accounts can be activated or deactivated here.");
        }
        user.setEnabled(enabled);
        return user;
    }
}
