package com.example.demo.controller;

import com.example.demo.exception.BusinessException;
import com.example.demo.model.OrderStatus;
import com.example.demo.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) OrderStatus status,
                       Model model) {
        model.addAttribute("orders", orderService.adminSearch(q, status));
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("query", q == null ? "" : q);
        model.addAttribute("selectedStatus", status);
        return "admin-orders";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.getOrderForAdmin(id));
        return "admin-order-detail";
    }

    /** Only transitions allowed by OrderStatus are accepted; cancelling puts the stock back. */
    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam OrderStatus status,
                               RedirectAttributes flash) {
        try {
            orderService.updateStatusByAdmin(id, status);
            flash.addFlashAttribute("success", "Order #" + id + " is now " + status.getLabel() + ".");
        } catch (BusinessException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }
}
