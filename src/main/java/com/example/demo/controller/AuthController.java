package com.example.demo.controller;

import com.example.demo.dto.RegistrationForm;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.DuplicateEmailException;
import com.example.demo.model.Role;
import com.example.demo.security.BooklyUserDetails;
import com.example.demo.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Registration and the login pages. The login form POSTs and logout are handled by
 * Spring Security (see SecurityConfig): it checks the BCrypt hash and the role.
 */
@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/register")
    public String registerPage(@AuthenticationPrincipal BooklyUserDetails user, Model model) {
        if (user != null) return "redirect:/";
        model.addAttribute("form", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegistrationForm form,
                           BindingResult result) {
        if (form.getPassword() != null && !form.getPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "mismatch", "Passwords do not match.");
        }
        if (result.hasErrors()) {
            return "register";
        }
        try {
            authService.registerCustomer(form);
        } catch (DuplicateEmailException e) {
            result.rejectValue("email", "duplicate", e.getMessage());
            return "register";
        } catch (BusinessException e) {
            result.reject("registration", e.getMessage());
            return "register";
        }
        return "redirect:/login?registered";
    }

    @GetMapping("/login")
    public String customerLoginPage(@AuthenticationPrincipal BooklyUserDetails user) {
        if (user != null && user.getRole() == Role.CUSTOMER) return "redirect:/";
        return "login";
    }

    @GetMapping("/admin/login")
    public String adminLoginPage(@AuthenticationPrincipal BooklyUserDetails user) {
        if (user != null && user.getRole() == Role.ADMIN) return "redirect:/admin/dashboard";
        return "admin-login";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
}
