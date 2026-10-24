package com.example.demo.controller;

import com.example.demo.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Values every page needs: cart badge count and the current URL (to come back after a form). */
@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute("cartCount")
    public int cartCount(HttpServletRequest request) {
        return CartService.itemCount(request.getSession(false));
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    @ModelAttribute("currentUrl")
    public String currentUrl(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String query = request.getQueryString();
        return query == null ? path : path + "?" + query;
    }
}
