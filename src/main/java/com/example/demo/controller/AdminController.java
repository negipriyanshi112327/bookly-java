package com.example.demo.controller;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.model.AppUser;
import com.example.demo.service.AdminService;
import com.example.demo.service.BookService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Dashboard, customers and inventory. Every /admin/** URL requires ROLE_ADMIN (SecurityConfig). */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final BookService bookService;

    public AdminController(AdminService adminService, BookService bookService) {
        this.adminService = adminService;
        this.bookService = bookService;
    }

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("stats", adminService.stats());
        model.addAttribute("recentOrders", adminService.recentOrders());
        model.addAttribute("lowStock", adminService.lowStock());
        return "admin-dashboard";
    }

    // ---------- customers ----------

    @GetMapping("/customers")
    public String customers(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("customers", adminService.customers(q));
        model.addAttribute("query", q == null ? "" : q);
        return "admin-customers";
    }

    @PostMapping("/customers/{id}/status")
    public String setCustomerStatus(@PathVariable Long id, @RequestParam boolean enabled,
                                    RedirectAttributes flash) {
        try {
            AppUser user = adminService.setCustomerEnabled(id, enabled);
            flash.addFlashAttribute("success", user.getName() + "'s account was " + (enabled ? "activated." : "deactivated."));
        } catch (BusinessException | NotFoundException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/customers";
    }

    // ---------- inventory ----------

    @GetMapping("/inventory")
    public String inventory(@RequestParam(defaultValue = "false") boolean low, Model model) {
        model.addAttribute("books", bookService.inventory(low));
        model.addAttribute("lowOnly", low);
        return "admin-inventory";
    }

    /** expected = stock shown on screen; the update is refused if an order changed it meanwhile. */
    @PostMapping("/inventory/{id}")
    public String updateStock(@PathVariable Long id,
                              @RequestParam int expected,
                              @RequestParam int stockQuantity,
                              @RequestParam(defaultValue = "false") boolean low,
                              RedirectAttributes flash) {
        try {
            bookService.changeStock(id, expected, stockQuantity);
            flash.addFlashAttribute("success", "Stock updated for \"" + bookService.getAnyBook(id).getName()
                    + "\" (" + stockQuantity + ").");
        } catch (BusinessException | NotFoundException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return low ? "redirect:/admin/inventory?low=true" : "redirect:/admin/inventory";
    }
}
