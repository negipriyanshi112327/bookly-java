package com.example.demo;

import com.example.demo.dto.CheckoutForm;
import com.example.demo.model.AppUser;
import com.example.demo.model.Book;
import com.example.demo.model.Role;
import com.example.demo.repository.BookRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.BooklyUserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.UUID;

/** Small helpers to create test records. */
public final class TestData {

    public static final String PASSWORD = "Secret123";

    private TestData() {}

    public static AppUser customer(UserRepository users, PasswordEncoder encoder) {
        AppUser user = new AppUser();
        user.setName("Test Reader");
        user.setEmail("reader-" + UUID.randomUUID() + "@test.local");
        user.setPassword(encoder.encode(PASSWORD));
        user.setRole(Role.CUSTOMER);
        user.setEnabled(true);
        return users.saveAndFlush(user);
    }

    public static Book book(BookRepository books, String name, String author, String category,
                            String price, int stock) {
        Book book = new Book(name, author, new BigDecimal(price), null);
        book.setCategory(category);
        book.setDescription("A test description about " + name.toLowerCase() + " and " + category.toLowerCase() + ".");
        book.setStockQuantity(stock);
        return books.saveAndFlush(book);
    }

    public static CheckoutForm checkoutForm() {
        CheckoutForm form = new CheckoutForm();
        form.setCustomerName("Test Reader");
        form.setPhone("9876543210");
        form.setAddress("12 Test Street, Goregaon East, Mumbai 400063");
        return form;
    }

    public static BooklyUserDetails principal(AppUser user) {
        return new BooklyUserDetails(user.getId(), user.getName(), user.getEmail(), "n/a", user.getRole(), true);
    }

    public static BooklyUserDetails admin(UserRepository users) {
        return new BooklyUserDetails(users.findByEmailIgnoreCase("admin@test.local").orElseThrow());
    }
}
