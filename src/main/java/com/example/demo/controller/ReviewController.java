package com.example.demo.controller;

import com.example.demo.dto.ReviewForm;
import com.example.demo.exception.BusinessException;
import com.example.demo.model.Book;
import com.example.demo.model.Review;
import com.example.demo.security.BooklyUserDetails;
import com.example.demo.service.BookService;
import com.example.demo.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/books/{id}/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final BookService bookService;

    public ReviewController(ReviewService reviewService, BookService bookService) {
        this.reviewService = reviewService;
        this.bookService = bookService;
    }

    /** Anyone can read reviews; the form is shown only to customers who received the book. */
    @GetMapping
    public String reviews(@PathVariable Long id, @AuthenticationPrincipal BooklyUserDetails user, Model model) {
        Book book = bookService.getActiveBook(id);
        ReviewForm form = new ReviewForm();
        if (HomeController.isCustomer(user)) {
            Optional<Review> own = reviewService.findOwn(user.getId(), id);
            own.ifPresent(r -> {
                form.setRating(r.getRating());
                form.setComment(r.getComment());
            });
            model.addAttribute("hasOwnReview", own.isPresent());
        }
        model.addAttribute("form", form);
        fillPage(id, book, user, model);
        return "reviews";
    }

    @PostMapping
    public String submit(@PathVariable Long id,
                         @AuthenticationPrincipal BooklyUserDetails user,
                         @Valid @ModelAttribute("form") ReviewForm form,
                         BindingResult result,
                         Model model,
                         RedirectAttributes flash) {
        Book book = bookService.getActiveBook(id);
        if (result.hasErrors()) {
            model.addAttribute("hasOwnReview", reviewService.findOwn(user.getId(), id).isPresent());
            fillPage(id, book, user, model);
            return "reviews";
        }
        try {
            boolean existed = reviewService.findOwn(user.getId(), id).isPresent();
            reviewService.save(user.getId(), id, form);
            flash.addFlashAttribute("success", existed ? "Your review was updated." : "Thank you! Your review was posted.");
        } catch (BusinessException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/books/" + id + "/reviews";
    }

    @PostMapping("/delete")
    public String delete(@PathVariable Long id, @AuthenticationPrincipal BooklyUserDetails user,
                         RedirectAttributes flash) {
        reviewService.deleteOwn(user.getId(), id);
        flash.addFlashAttribute("success", "Your review was deleted.");
        return "redirect:/books/" + id + "/reviews";
    }

    private void fillPage(Long id, Book book, BooklyUserDetails user, Model model) {
        model.addAttribute("book", book);
        model.addAttribute("reviews", reviewService.forBook(id));
        model.addAttribute("rating", reviewService.summary(id));
        model.addAttribute("canReview", HomeController.isCustomer(user) && reviewService.hasPurchased(user.getId(), id));
        model.addAttribute("currentUserId", user == null ? null : user.getId());
    }
}
