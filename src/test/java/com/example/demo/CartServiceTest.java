package com.example.demo;

import com.example.demo.dto.CartView;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.model.Book;
import com.example.demo.repository.BookRepository;
import com.example.demo.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class CartServiceTest {

    @Autowired CartService cartService;
    @Autowired BookRepository books;

    MockHttpSession session;
    Book habits, money, soldOut;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        habits = TestData.book(books, "Atomic Habits", "James Clear", "Self-Help", "399.00", 5);
        money = TestData.book(books, "The Psychology of Money", "Morgan Housel", "Finance", "349.50", 20);
        soldOut = TestData.book(books, "Wings of Fire", "A.P.J. Abdul Kalam", "Biography", "299.00", 0);
    }

    @Test
    void addUpdateRemoveAndTotals() {
        cartService.add(session, habits.getId(), 1);
        cartService.add(session, habits.getId(), 1);
        cartService.add(session, money.getId(), 1);

        CartView view = cartService.view(session);
        assertThat(view.getTotalItems()).isEqualTo(3);
        assertThat(view.getTotal()).isEqualByComparingTo("1147.50"); // 2 x 399 + 349.50

        cartService.update(session, money.getId(), 3);
        assertThat(cartService.view(session).getTotal()).isEqualByComparingTo("1846.50"); // 798 + 1048.50

        cartService.remove(session, habits.getId());
        CartView after = cartService.view(session);
        assertThat(after.getLines()).hasSize(1);
        assertThat(after.getTotal()).isEqualByComparingTo("1048.50");
    }

    @Test
    void totalsUseTheCurrentDatabasePrice() {
        cartService.add(session, habits.getId(), 2);
        habits.setPrice(new BigDecimal("100.00"));
        books.saveAndFlush(habits);
        assertThat(cartService.view(session).getTotal()).isEqualByComparingTo("200.00");
    }

    @Test
    void zeroOrNegativeQuantitiesAreRejected() {
        cartService.add(session, habits.getId(), 1);
        assertThatThrownBy(() -> cartService.update(session, habits.getId(), 0)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> cartService.update(session, habits.getId(), -2)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> cartService.add(session, habits.getId(), 0)).isInstanceOf(BusinessException.class);
    }

    @Test
    void quantityAboveStockIsRejectedOrCapped() {
        assertThatThrownBy(() -> {
            cartService.add(session, habits.getId(), 1);
            cartService.update(session, habits.getId(), 6); // only 5 in stock
        }).isInstanceOf(BusinessException.class);

        cartService.add(session, money.getId(), 50);    // capped at MAX_PER_BOOK
        assertThat(cartService.snapshot(session).get(money.getId())).isEqualTo(CartService.MAX_PER_BOOK);
    }

    @Test
    void outOfStockOrUnknownBooksCannotBeAdded() {
        assertThatThrownBy(() -> cartService.add(session, soldOut.getId(), 1)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> cartService.add(session, 999_999L, 1)).isInstanceOf(NotFoundException.class);
        assertThat(cartService.snapshot(session)).isEmpty();
    }

    @Test
    void deletedBooksDisappearFromTheCart() {
        cartService.add(session, money.getId(), 1);
        money.setActive(false);
        books.saveAndFlush(money);
        CartView view = cartService.view(session);
        assertThat(view.isEmpty()).isTrue();
        assertThat(view.getNotices()).isNotEmpty();
    }
}
