package com.example.demo;

import com.example.demo.dto.RegistrationForm;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.DuplicateEmailException;
import com.example.demo.model.AppUser;
import com.example.demo.model.Role;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AuthServiceTest {

    @Autowired AuthService authService;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    private RegistrationForm form(String email) {
        RegistrationForm form = new RegistrationForm();
        form.setName("Priya Sharma");
        form.setEmail(email);
        form.setPassword("Reading2026");
        form.setConfirmPassword("Reading2026");
        return form;
    }

    @Test
    void registrationCreatesCustomerWithBcryptHash() {
        AppUser user = authService.registerCustomer(form("  Priya@Example.com "));

        assertThat(user.getId()).isNotNull();
        assertThat(user.getEmail()).isEqualTo("priya@example.com");
        assertThat(user.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(user.isEnabled()).isTrue();
        // 4. password hash verification
        assertThat(user.getPassword()).isNotEqualTo("Reading2026").startsWith("$2");
        assertThat(encoder.matches("Reading2026", user.getPassword())).isTrue();
        assertThat(encoder.matches("wrong-password1", user.getPassword())).isFalse();
    }

    @Test
    void duplicateEmailIsRejectedIgnoringCase() {
        authService.registerCustomer(form("dup@example.com"));

        assertThatThrownBy(() -> authService.registerCustomer(form("DUP@example.com")))
                .isInstanceOf(DuplicateEmailException.class);
        assertThat(users.findAll().stream().filter(u -> u.getEmail().equals("dup@example.com"))).hasSize(1);
    }

    @Test
    void mismatchedPasswordsAreRejected() {
        RegistrationForm form = form("mismatch@example.com");
        form.setConfirmPassword("Different2026");

        assertThatThrownBy(() -> authService.registerCustomer(form)).isInstanceOf(BusinessException.class);
        assertThat(users.existsByEmailIgnoreCase("mismatch@example.com")).isFalse();
    }
}
