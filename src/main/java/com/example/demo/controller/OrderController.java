package com.example.demo.controller;

import com.example.demo.dto.CartView;
import com.example.demo.dto.CheckoutForm;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.model.CustomerOrder;
import com.example.demo.model.PaymentMethod;
import com.example.demo.security.BooklyUserDetails;
import com.example.demo.service.CartService;
import com.example.demo.service.OrderService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
import java.util.UUID;

/** Checkout (Cash on Delivery), confirmation, order history and cancellation. Customers only. */
@Controller
public class OrderController {

    static final String CHECKOUT_TOKEN = "checkoutToken";

    private final CartService cartService;
    private final OrderService orderService;

    public OrderController(CartService cartService, OrderService orderService) {
        this.cartService = cartService;
        this.orderService = orderService;
    }

    @GetMapping("/checkout")
    public String checkoutPage(@AuthenticationPrincipal BooklyUserDetails user,
                               HttpSession session, Model model, RedirectAttributes flash) {
        CartView cart = cartService.view(session);
        if (cart.isEmpty()) {
            flash.addFlashAttribute("error", "Your cart is empty. Add a book before checking out.");
            return "redirect:/cart";
        }
        if (!cart.isCheckoutReady()) {
            flash.addFlashAttribute("error", "Please fix the items marked in your cart before checking out.");
            return "redirect:/cart";
        }
        CheckoutForm form = new CheckoutForm();
        form.setCustomerName(user.getName());
        model.addAttribute("form", form);
        prepareCheckoutPage(session, model, cart);
        return "checkout";
    }

    @PostMapping("/checkout")
    public String placeOrder(@AuthenticationPrincipal BooklyUserDetails user,
                             @Valid @ModelAttribute("form") CheckoutForm form,
                             BindingResult result,
                             @RequestParam(name = "checkoutToken", required = false) String token,
                             HttpSession session, Model model, RedirectAttributes flash) {

        // the same form submitted twice (double click / refresh): show the order that already exists
        Optional<CustomerOrder> previous = orderService.findByCheckoutToken(token, user.getId());
        if (previous.isPresent()) {
            return "redirect:/orders/" + previous.get().getId() + "/confirmation";
        }
        if (token == null || !token.equals(session.getAttribute(CHECKOUT_TOKEN))) {
            flash.addFlashAttribute("error", "Your checkout page expired. Please review your order and try again.");
            return "redirect:/checkout";
        }

        CartView cart = cartService.view(session);
        if (cart.isEmpty() || !cart.isCheckoutReady()) {
            flash.addFlashAttribute("error", "Your cart changed. Please review it before placing the order.");
            return "redirect:/cart";
        }
        if (result.hasErrors()) {
            prepareCheckoutPage(session, model, cart);
            return "checkout";
        }

        try {
            CustomerOrder order = orderService.placeOrder(user.getId(), form, cartService.snapshot(session), token);
            cartService.clear(session);              // only after the order was saved
            session.removeAttribute(CHECKOUT_TOKEN);
            return "redirect:/orders/" + order.getId() + "/confirmation";
        } catch (BusinessException e) {
            // transaction rolled back, stock untouched, cart kept
            model.addAttribute("error", e.getMessage());
            prepareCheckoutPage(session, model, cartService.view(session));
            return "checkout";
        } catch (DataIntegrityViolationException e) {
            // a parallel submit with the same token won the race
            return orderService.findByCheckoutToken(token, user.getId())
                    .map(o -> "redirect:/orders/" + o.getId() + "/confirmation")
                    .orElse("redirect:/checkout");
        }
    }

    @GetMapping("/orders/{id}/confirmation")
    public String confirmation(@PathVariable Long id, @AuthenticationPrincipal BooklyUserDetails user, Model model) {
        model.addAttribute("order", orderService.getOrderForCustomer(user.getId(), id));
        return "order-confirmation";
    }

    @GetMapping("/orders")
    public String history(@AuthenticationPrincipal BooklyUserDetails user, Model model) {
        model.addAttribute("orders", orderService.ordersForCustomer(user.getId()));
        return "order-history";
    }

    @GetMapping("/orders/{id}")
    public String detail(@PathVariable Long id, @AuthenticationPrincipal BooklyUserDetails user, Model model) {
        model.addAttribute("order", orderService.getOrderForCustomer(user.getId(), id));
        return "order-detail";
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancel(@PathVariable Long id, @AuthenticationPrincipal BooklyUserDetails user,
                         RedirectAttributes flash) {
        try {
            orderService.cancelByCustomer(user.getId(), id);
            flash.addFlashAttribute("success", "Order #" + id + " was cancelled.");
        } catch (NotFoundException e) {
            throw e;
        } catch (BusinessException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    private void prepareCheckoutPage(HttpSession session, Model model, CartView cart) {
        String token = (String) session.getAttribute(CHECKOUT_TOKEN);
        if (token == null) {
            token = UUID.randomUUID().toString();
            session.setAttribute(CHECKOUT_TOKEN, token);
        }
        model.addAttribute("checkoutToken", token);
        model.addAttribute("cart", cart);
        model.addAttribute("paymentMethods", PaymentMethod.values());
    }
}
