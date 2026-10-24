package com.example.demo.controller;

import com.example.demo.service.BookCompassService;
import com.example.demo.service.BookService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
public class BookCompassController {

    private final BookCompassService compassService;
    private final BookService bookService;

    public BookCompassController(BookCompassService compassService, BookService bookService) {
        this.compassService = compassService;
        this.bookService = bookService;
    }

    @GetMapping("/book-compass")
    public String compass(@RequestParam(required = false) String category,
                          @RequestParam(required = false) String author,
                          @RequestParam(required = false) String interests,
                          @RequestParam(required = false) BigDecimal maxPrice,
                          @RequestParam(required = false) String go,
                          Model model) {
        model.addAttribute("categories", bookService.categories());
        model.addAttribute("category", category == null ? "" : category);
        model.addAttribute("author", author == null ? "" : author);
        model.addAttribute("interests", interests == null ? "" : interests);
        model.addAttribute("maxPrice", maxPrice);

        if (go != null) {
            boolean anyPreference = !isBlank(category) || !isBlank(author) || !isBlank(interests);
            if (!anyPreference) {
                model.addAttribute("formError", "Tell us at least one thing: a category, an author or an interest.");
            } else if (maxPrice != null && maxPrice.signum() <= 0) {
                model.addAttribute("formError", "Budget must be more than ₹0.");
            } else {
                model.addAttribute("result", compassService.recommend(category, author, interests, maxPrice));
            }
        }
        return "book-compass";
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
