package com.example.demo.controller;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.security.BooklyUserDetails;
import com.example.demo.service.WishlistService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Every query uses the logged-in user's id, so customers only ever see their own wishlist. */
@Controller
@RequestMapping("/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public String view(@AuthenticationPrincipal BooklyUserDetails user, Model model) {
        model.addAttribute("items", wishlistService.list(user.getId()));
        return "wishlist";
    }

    @PostMapping("/add/{bookId}")
    public String add(@PathVariable Long bookId, @AuthenticationPrincipal BooklyUserDetails user,
                      @RequestParam(required = false) String redirectTo, RedirectAttributes flash) {
        try {
            flash.addFlashAttribute("success", wishlistService.add(user.getId(), bookId));
        } catch (BusinessException | NotFoundException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return SafeRedirect.to(redirectTo, "/wishlist");
    }

    @PostMapping("/remove/{bookId}")
    public String remove(@PathVariable Long bookId, @AuthenticationPrincipal BooklyUserDetails user,
                         @RequestParam(required = false) String redirectTo, RedirectAttributes flash) {
        try {
            wishlistService.remove(user.getId(), bookId);
            flash.addFlashAttribute("success", "Removed from your wishlist.");
        } catch (NotFoundException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return SafeRedirect.to(redirectTo, "/wishlist");
    }

    @PostMapping("/move-to-cart/{bookId}")
    public String moveToCart(@PathVariable Long bookId, @AuthenticationPrincipal BooklyUserDetails user,
                             HttpSession session, RedirectAttributes flash) {
        try {
            flash.addFlashAttribute("success", wishlistService.moveToCart(user.getId(), bookId, session));
            flash.addFlashAttribute("showCartLink", true);
        } catch (BusinessException | NotFoundException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/wishlist";
    }
}
