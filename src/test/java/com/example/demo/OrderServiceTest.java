package com.example.demo;

import com.example.demo.exception.InsufficientStockException;
import com.example.demo.exception.InvalidStatusTransitionException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Not @Transactional on purpose: each service call commits or rolls back for real,
 * so we can prove that a failed checkout leaves stock and orders untouched.
 */
@SpringBootTest
class OrderServiceTest {

    @Autowired OrderService orderService;
    @Autowired CustomerOrderRepository orders;
    @Autowired BookRepository books;
    @Autowired UserRepository users;
    @Autowired ReviewRepository reviews;
    @Autowired WishlistRepository wishlists;
    @Autowired PasswordEncoder encoder;

    AppUser customer, otherCustomer;
    Book habits, money;

    @BeforeEach
    void setUp() {
        cleanUp();
        customer = TestData.customer(users, encoder);
        otherCustomer = TestData.customer(users, encoder);
        habits = TestData.book(books, "Atomic Habits", "James Clear", "Self-Help", "399.00", 5);
        money = TestData.book(books, "The Psychology of Money", "Morgan Housel", "Finance", "349.50", 2);
    }

    @AfterEach
    void cleanUp() {
        reviews.deleteAll();
        wishlists.deleteAll();
        orders.deleteAll();
        books.deleteAll();
        users.findAll().stream().filter(u -> u.getRole() == Role.CUSTOMER).forEach(users::delete);
    }

    private Map<Long, Integer> cart(Object... idQty) {
        Map<Long, Integer> cart = new LinkedHashMap<>();
        for (int i = 0; i < idQty.length; i += 2) cart.put((Long) idQty[i], (Integer) idQty[i + 1]);
        return cart;
    }

    private int stock(Book book) {
        return books.findStockById(book.getId()).orElseThrow();
    }

    @Test
    void checkoutSavesOrderItemsAndReducesStock() {
        CustomerOrder order = orderService.placeOrder(customer.getId(), TestData.checkoutForm(),
                cart(habits.getId(), 2, money.getId(), 1), UUID.randomUUID().toString());

        CustomerOrder saved = orderService.getOrderForCustomer(customer.getId(), order.getId());
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(saved.getPaymentMethod()).isEqualTo(PaymentMethod.COD);
        assertThat(saved.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("1147.50");
        assertThat(saved.getItems()).hasSize(2);
        assertThat(saved.getItems()).anySatisfy(item -> {
            assertThat(item.getBookName()).isEqualTo("Atomic Habits");
            assertThat(item.getQuantity()).isEqualTo(2);
            assertThat(item.getUnitPrice()).isEqualByComparingTo("399.00");
            assertThat(item.getSubtotal()).isEqualByComparingTo("798.00");
        });

        // 10. inventory updated
        assertThat(stock(habits)).isEqualTo(3);
        assertThat(stock(money)).isEqualTo(1);
    }

    @Test
    void priceSnapshotSurvivesCatalogueChanges() {
        CustomerOrder order = orderService.placeOrder(customer.getId(), TestData.checkoutForm(),
                cart(habits.getId(), 1), UUID.randomUUID().toString());
        Book changed = books.findById(habits.getId()).orElseThrow();
        changed.setName("Atomic Habits (New Edition)");
        changed.setPrice(new java.math.BigDecimal("999.00"));
        books.saveAndFlush(changed);

        OrderItem item = orderService.getOrderForCustomer(customer.getId(), order.getId()).getItems().get(0);
        assertThat(item.getBookName()).isEqualTo("Atomic Habits");
        assertThat(item.getUnitPrice()).isEqualByComparingTo("399.00");
    }

    @Test
    void insufficientStockRejectsCheckoutAndRollsBackEverything() {
        long ordersBefore = orders.count();

        assertThatThrownBy(() -> orderService.placeOrder(customer.getId(), TestData.checkoutForm(),
                cart(habits.getId(), 2, money.getId(), 3), UUID.randomUUID().toString()))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Psychology of Money");

        // habits was reserved first, then money failed: the whole transaction is rolled back
        assertThat(stock(habits)).isEqualTo(5);
        assertThat(stock(money)).isEqualTo(2);
        assertThat(orders.count()).isEqualTo(ordersBefore);
    }

    @Test
    void sameCheckoutTokenCreatesOnlyOneOrder() {
        String token = UUID.randomUUID().toString();
        CustomerOrder first = orderService.placeOrder(customer.getId(), TestData.checkoutForm(), cart(habits.getId(), 1), token);
        CustomerOrder second = orderService.placeOrder(customer.getId(), TestData.checkoutForm(), cart(habits.getId(), 1), token);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(stock(habits)).isEqualTo(4);
    }

    @Test
    void customersCannotSeeOtherCustomersOrders() {
        CustomerOrder order = orderService.placeOrder(customer.getId(), TestData.checkoutForm(),
                cart(habits.getId(), 1), UUID.randomUUID().toString());

        assertThatThrownBy(() -> orderService.getOrderForCustomer(otherCustomer.getId(), order.getId()))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> orderService.cancelByCustomer(otherCustomer.getId(), order.getId()))
                .isInstanceOf(NotFoundException.class);
        assertThat(orderService.ordersForCustomer(otherCustomer.getId())).isEmpty();
    }

    @Test
    void invalidStatusTransitionsAreRejected() {
        CustomerOrder order = orderService.placeOrder(customer.getId(), TestData.checkoutForm(),
                cart(habits.getId(), 1), UUID.randomUUID().toString());
        Long id = order.getId();

        assertThatThrownBy(() -> orderService.updateStatusByAdmin(id, OrderStatus.DELIVERED))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThatThrownBy(() -> orderService.updateStatusByAdmin(id, OrderStatus.PENDING))
                .isInstanceOf(InvalidStatusTransitionException.class);

        orderService.updateStatusByAdmin(id, OrderStatus.CONFIRMED);
        orderService.updateStatusByAdmin(id, OrderStatus.SHIPPED);

        // shipped orders can no longer be cancelled by the customer or the admin
        assertThatThrownBy(() -> orderService.cancelByCustomer(customer.getId(), id))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThatThrownBy(() -> orderService.updateStatusByAdmin(id, OrderStatus.CANCELLED))
                .isInstanceOf(InvalidStatusTransitionException.class);

        CustomerOrder delivered = orderService.updateStatusByAdmin(id, OrderStatus.DELIVERED);
        assertThat(delivered.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThatThrownBy(() -> orderService.updateStatusByAdmin(id, OrderStatus.CANCELLED))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void cancellingRestoresStock() {
        CustomerOrder order = orderService.placeOrder(customer.getId(), TestData.checkoutForm(),
                cart(habits.getId(), 3), UUID.randomUUID().toString());
        assertThat(stock(habits)).isEqualTo(2);

        CustomerOrder cancelled = orderService.cancelByCustomer(customer.getId(), order.getId());

        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(stock(habits)).isEqualTo(5);
    }
}
