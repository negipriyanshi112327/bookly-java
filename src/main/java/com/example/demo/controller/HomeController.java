package com.example.demo.controller;

import com.example.demo.model.Book;
import com.example.demo.model.Review;
import com.example.demo.model.Role;
import com.example.demo.security.BooklyUserDetails;
import com.example.demo.service.BookService;
import com.example.demo.service.ReviewService;
import com.example.demo.service.WishlistService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;

@Controller
public class HomeController {

    private final BookService bookService;
    private final ReviewService reviewService;
    private final WishlistService wishlistService;

    public HomeController(BookService bookService, ReviewService reviewService, WishlistService wishlistService) {
        this.bookService = bookService;
        this.reviewService = reviewService;
        this.wishlistService = wishlistService;
    }

    @GetMapping("/")
    public String home(@RequestParam(required = false) String q,
                       @RequestParam(required = false) String category,
                       @RequestParam(required = false) String sort,
                       @AuthenticationPrincipal BooklyUserDetails user,
                       Model model) {

        boolean filtered = (q != null && !q.isBlank()) || (category != null && !category.isBlank());
        List<Book> books = bookService.catalogue(q, category, sort);

        model.addAttribute("books", books);
        model.addAttribute("featured", filtered ? List.of() : bookService.featured());
        model.addAttribute("categories", bookService.categories());
        model.addAttribute("sortOptions", BookService.SortOption.values());
        model.addAttribute("query", q == null ? "" : q.trim());
        model.addAttribute("selectedCategory", category == null ? "" : category);
        model.addAttribute("selectedSort", BookService.SortOption.from(sort).getKey());
        model.addAttribute("filtered", filtered);
        model.addAttribute("ratings", reviewService.summaries());
        model.addAttribute("wishlistIds", wishlistIds(user));
        return "index";
    }

    @GetMapping("/books/{id}")
    public String bookDetail(@PathVariable Long id,
                             @AuthenticationPrincipal BooklyUserDetails user,
                             Model model) {
        Book book = bookService.getActiveBook(id);
        List<Review> reviews = reviewService.forBook(id);

        model.addAttribute("book", book);
        model.addAttribute("rating", reviewService.summary(id));
        model.addAttribute("latestReviews", reviews.stream().limit(3).toList());
        model.addAttribute("related", bookService.related(book));
        model.addAttribute("ratings", reviewService.summaries());
        model.addAttribute("wishlistIds", wishlistIds(user));
        model.addAttribute("canReview", isCustomer(user) && reviewService.hasPurchased(user.getId(), id));
        return "book-detail";
    }

    private Set<Long> wishlistIds(BooklyUserDetails user) {
        return isCustomer(user) ? wishlistService.bookIds(user.getId()) : Set.of();
    }

    static boolean isCustomer(BooklyUserDetails user) {
        return user != null && user.getRole() == Role.CUSTOMER;
    }
}
