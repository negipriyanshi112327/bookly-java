package com.example.demo;

import com.example.demo.dto.ReviewForm;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.model.AppUser;
import com.example.demo.model.Book;
import com.example.demo.model.CustomerOrder;
import com.example.demo.model.OrderStatus;
import com.example.demo.repository.BookRepository;
import com.example.demo.repository.ReviewRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.OrderService;
import com.example.demo.service.ReviewService;
import com.example.demo.service.WishlistService;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class WishlistAndReviewTest {

    @Autowired WishlistService wishlistService;
    @Autowired ReviewService reviewService;
    @Autowired OrderService orderService;
    @Autowired ReviewRepository reviews;
    @Autowired BookRepository books;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired Validator validator;

    AppUser customer, other;
    Book book;

    @BeforeEach
    void setUp() {
        customer = TestData.customer(users, encoder);
        other = TestData.customer(users, encoder);
        book = TestData.book(books, "The Hobbit", "J.R.R. Tolkien", "Fantasy", "449.00", 10);
    }

    private ReviewForm review(Integer rating, String comment) {
        ReviewForm form = new ReviewForm();
        form.setRating(rating);
        form.setComment(comment);
        return form;
    }

    private void deliverBookTo(AppUser user) {
        CustomerOrder order = orderService.placeOrder(user.getId(), TestData.checkoutForm(),
                Map.of(book.getId(), 1), UUID.randomUUID().toString());
        orderService.updateStatusByAdmin(order.getId(), OrderStatus.CONFIRMED);
        orderService.updateStatusByAdmin(order.getId(), OrderStatus.SHIPPED);
        orderService.updateStatusByAdmin(order.getId(), OrderStatus.DELIVERED);
    }

    // ---------- wishlist ----------

    @Test
    void wishlistPreventsDuplicates() {
        wishlistService.add(customer.getId(), book.getId());
        assertThatThrownBy(() -> wishlistService.add(customer.getId(), book.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already");
        assertThat(wishlistService.list(customer.getId())).hasSize(1);
    }

    @Test
    void wishlistsArePrivateToEachCustomer() {
        wishlistService.add(customer.getId(), book.getId());
        assertThat(wishlistService.list(other.getId())).isEmpty();
        assertThatThrownBy(() -> wishlistService.remove(other.getId(), book.getId()))
                .isInstanceOf(NotFoundException.class);
        assertThat(wishlistService.contains(customer.getId(), book.getId())).isTrue();
    }

    // ---------- reviews ----------

    @Test
    void reviewFormValidation() {
        assertThat(validator.validate(review(6, "Lovely adventure story."))).isNotEmpty();
        assertThat(validator.validate(review(0, "Lovely adventure story."))).isNotEmpty();
        assertThat(validator.validate(review(5, "short"))).isNotEmpty();
        assertThat(validator.validate(review(null, "Lovely adventure story."))).isNotEmpty();
        assertThat(validator.validate(review(4, "Lovely adventure story."))).isEmpty();
    }

    @Test
    void onlyVerifiedPurchasersCanReview() {
        assertThatThrownBy(() -> reviewService.save(customer.getId(), book.getId(), review(5, "Wonderful from start to end.")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("received");

        deliverBookTo(customer);
        reviewService.save(customer.getId(), book.getId(), review(5, "Wonderful from start to end."));
        assertThat(reviewService.summary(book.getId()).getAverage()).isEqualTo(5.0);
    }

    @Test
    void secondReviewUpdatesInsteadOfDuplicating() {
        deliverBookTo(customer);
        reviewService.save(customer.getId(), book.getId(), review(5, "Wonderful from start to end."));
        reviewService.save(customer.getId(), book.getId(), review(3, "Re-read it, still good but slow."));

        assertThat(reviews.findByBookIdOrderByCreatedAtDesc(book.getId())).hasSize(1);
        assertThat(reviewService.summary(book.getId()).getAverage()).isEqualTo(3.0);
    }

    @Test
    void customersCanOnlyDeleteTheirOwnReview() {
        deliverBookTo(customer);
        reviewService.save(customer.getId(), book.getId(), review(4, "A cosy and clever adventure."));

        assertThatThrownBy(() -> reviewService.deleteOwn(other.getId(), book.getId()))
                .isInstanceOf(NotFoundException.class);
        reviewService.deleteOwn(customer.getId(), book.getId());
        assertThat(reviews.findByBookIdOrderByCreatedAtDesc(book.getId())).isEmpty();
    }

    @Test
    void outOfRangeRatingIsRejectedByTheService() {
        deliverBookTo(customer);
        assertThatThrownBy(() -> reviewService.save(customer.getId(), book.getId(), review(9, "Not a valid rating at all.")))
                .isInstanceOf(BusinessException.class);
    }
}
