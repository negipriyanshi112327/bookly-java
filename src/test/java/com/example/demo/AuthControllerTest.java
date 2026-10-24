package com.example.demo;

import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    @Test
    void registrationPageRenders() throws Exception {
        mvc.perform(get("/register")).andExpect(status().isOk()).andExpect(view().name("register"));
    }

    @Test
    void successfulRegistrationRedirectsToLogin() throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("name", "Arjun Mehta")
                        .param("email", "arjun@example.com")
                        .param("password", "Books2026")
                        .param("confirmPassword", "Books2026"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        assertThat(users.existsByEmailIgnoreCase("arjun@example.com")).isTrue();
    }

    @Test
    void registrationWithoutCsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/register")
                        .param("name", "No Token")
                        .param("email", "notoken@example.com")
                        .param("password", "Books2026")
                        .param("confirmPassword", "Books2026"))
                .andExpect(status().isForbidden());
        assertThat(users.existsByEmailIgnoreCase("notoken@example.com")).isFalse();
    }

    @Test
    void duplicateEmailShowsFieldError() throws Exception {
        var existing = TestData.customer(users, encoder);
        mvc.perform(post("/register").with(csrf())
                        .param("name", "Copy Cat")
                        .param("email", existing.getEmail().toUpperCase())
                        .param("password", "Books2026")
                        .param("confirmPassword", "Books2026"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("form", "email"));
    }

    @Test
    void invalidRegistrationShowsValidationErrors() throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("name", "")
                        .param("email", "not-an-email")
                        .param("password", "short")
                        .param("confirmPassword", "other"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "name", "email", "password", "confirmPassword"));
    }

    @Test
    void customerCanLoginWithValidCredentials() throws Exception {
        var customer = TestData.customer(users, encoder);
        mvc.perform(formLogin("/login").userParameter("email").user(customer.getEmail()).password(TestData.PASSWORD))
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withRoles("CUSTOMER"));
    }

    @Test
    void loginWithWrongPasswordFails() throws Exception {
        var customer = TestData.customer(users, encoder);
        mvc.perform(formLogin("/login").userParameter("email").user(customer.getEmail()).password("WrongPass1"))
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void deactivatedCustomerCannotLogin() throws Exception {
        var customer = TestData.customer(users, encoder);
        customer.setEnabled(false);
        users.saveAndFlush(customer);
        mvc.perform(formLogin("/login").userParameter("email").user(customer.getEmail()).password(TestData.PASSWORD))
                .andExpect(redirectedUrl("/login?disabled"))
                .andExpect(unauthenticated());
    }

    @Test
    void adminCannotUseCustomerLoginForm() throws Exception {
        mvc.perform(formLogin("/login").userParameter("email").user("admin@test.local").password("AdminPass123"))
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void adminLoginUsesHashedConfiguredPassword() throws Exception {
        var admin = users.findByEmailIgnoreCase("admin@test.local").orElseThrow();
        assertThat(admin.getPassword()).isNotEqualTo("AdminPass123");

        mvc.perform(formLogin("/admin/login").userParameter("email").user("admin@test.local").password("AdminPass123"))
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(authenticated().withRoles("ADMIN"));
    }

    @Test
    void customerCannotUseAdminLoginForm() throws Exception {
        var customer = TestData.customer(users, encoder);
        mvc.perform(formLogin("/admin/login").userParameter("email").user(customer.getEmail()).password(TestData.PASSWORD))
                .andExpect(redirectedUrl("/admin/login?error"))
                .andExpect(unauthenticated());
    }
}
