package com.example.demo.controller;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Guests and customers can use the cart; checkout needs a customer login. */
@Controller
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/cart")
    public String showCart(HttpSession session, Model model) {
        model.addAttribute("cart", cartService.view(session));
        return "cart";
    }

    @PostMapping("/cart/add/{id}")
    public String add(@PathVariable Long id,
                      @RequestParam(defaultValue = "1") int quantity,
                      @RequestParam(required = false) String redirectTo,
                      HttpSession session,
                      RedirectAttributes flash) {
        try {
            flash.addFlashAttribute("success", cartService.add(session, id, quantity));
            flash.addFlashAttribute("showCartLink", true);
        } catch (BusinessException | NotFoundException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return SafeRedirect.to(redirectTo, "/cart");
    }

    @PostMapping("/cart/update/{id}")
    public String update(@PathVariable Long id,
                         @RequestParam int quantity,
                         HttpSession session,
                         RedirectAttributes flash) {
        try {
            cartService.update(session, id, quantity);
        } catch (BusinessException | NotFoundException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove/{id}")
    public String remove(@PathVariable Long id, HttpSession session, RedirectAttributes flash) {
        cartService.remove(session, id);
        flash.addFlashAttribute("success", "The book was removed from your cart.");
        return "redirect:/cart";
    }
}
