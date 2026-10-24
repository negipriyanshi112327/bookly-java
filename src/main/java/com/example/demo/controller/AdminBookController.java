package com.example.demo.controller;

import com.example.demo.dto.BookForm;
import com.example.demo.exception.BusinessException;
import com.example.demo.model.Book;
import com.example.demo.service.BookService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Admin catalogue management: list, search, add, edit, remove (delete or archive), restore. */
@Controller
@RequestMapping("/admin/books")
public class AdminBookController {

    private final BookService bookService;

    public AdminBookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) String category,
                       Model model) {
        model.addAttribute("books", bookService.adminSearch(q, category));
        model.addAttribute("categories", bookService.allCategories());
        model.addAttribute("query", q == null ? "" : q);
        model.addAttribute("selectedCategory", category == null ? "" : category);
        return "admin-books";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new BookForm());
        model.addAttribute("categories", bookService.allCategories());
        return "admin-book-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") BookForm form, BindingResult result,
                         Model model, RedirectAttributes flash) {
        if (result.hasErrors()) {
            model.addAttribute("categories", bookService.allCategories());
            return "admin-book-form";
        }
        Book book = bookService.create(form);
        flash.addFlashAttribute("success", "\"" + book.getName() + "\" was added to the catalogue.");
        return "redirect:/admin/books";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Book book = bookService.getAnyBook(id);
        model.addAttribute("form", BookForm.from(book));
        model.addAttribute("book", book);
        model.addAttribute("categories", bookService.allCategories());
        return "admin-book-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") BookForm form,
                         BindingResult result, Model model, RedirectAttributes flash) {
        Book existing = bookService.getAnyBook(id);
        form.setId(id);
        if (result.hasErrors()) {
            model.addAttribute("book", existing);
            model.addAttribute("categories", bookService.allCategories());
            return "admin-book-form";
        }
        try {
            Book book = bookService.update(id, form);
            flash.addFlashAttribute("success", "\"" + book.getName() + "\" was updated.");
            return "redirect:/admin/books";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("book", bookService.getAnyBook(id));
            model.addAttribute("categories", bookService.allCategories());
            return "admin-book-form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        String name = bookService.getAnyBook(id).getName();
        BookService.RemovalResult result = bookService.deleteOrArchive(id);
        if (result == BookService.RemovalResult.DELETED) {
            flash.addFlashAttribute("success", "\"" + name + "\" was deleted.");
        } else {
            flash.addFlashAttribute("success", "\"" + name + "\" appears in past orders, so it was archived: "
                    + "hidden from the store but kept for order history. You can restore it at any time.");
        }
        return "redirect:/admin/books";
    }

    @PostMapping("/{id}/restore")
    public String restore(@PathVariable Long id, RedirectAttributes flash) {
        bookService.restore(id);
        flash.addFlashAttribute("success", "\"" + bookService.getAnyBook(id).getName() + "\" is back in the store.");
        return "redirect:/admin/books";
    }
}
