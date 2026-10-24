package com.example.demo.service;

import com.example.demo.dto.RegistrationForm;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.DuplicateEmailException;
import com.example.demo.model.AppUser;
import com.example.demo.model.Role;
import com.example.demo.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AuthService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /** Creates a CUSTOMER account. The password is stored only as a BCrypt hash. */
    @Transactional
    public AppUser registerCustomer(RegistrationForm form) {
        if (form.getPassword() == null || !form.getPassword().equals(form.getConfirmPassword())) {
            throw new BusinessException("Passwords do not match.");
        }
        String email = normalizeEmail(form.getEmail());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException();
        }

        AppUser user = new AppUser();
        user.setName(form.getName().trim());
        user.setEmail(email);
        user.setPassword(encoder.encode(form.getPassword()));
        user.setRole(Role.CUSTOMER);
        user.setEnabled(true);

        try {
            return users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // two sign-ups with the same email at the same moment: the unique index wins
            throw new DuplicateEmailException();
        }
    }

    /** Loads an active customer for shop operations (rejects deactivated accounts). */
    @Transactional(readOnly = true)
    public AppUser requireActiveCustomer(Long userId) {
        AppUser user = users.findById(userId)
                .orElseThrow(() -> new BusinessException("Your account could not be found. Please log in again."));
        if (user.getRole() != Role.CUSTOMER) {
            throw new BusinessException("Only customer accounts can do this.");
        }
        if (!user.isEnabled()) {
            throw new BusinessException("Your account has been deactivated. Please contact the store.");
        }
        return user;
    }
}
