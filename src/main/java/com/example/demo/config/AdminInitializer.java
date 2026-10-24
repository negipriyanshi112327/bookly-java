package com.example.demo.config;

import com.example.demo.model.AppUser;
import com.example.demo.model.Role;
import com.example.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Locale;

/**
 * Creates (or updates) the administrator account from environment variables:
 *   BOOKLY_ADMIN_EMAIL, BOOKLY_ADMIN_PASSWORD, optional BOOKLY_ADMIN_NAME
 * Nothing is hardcoded. The password is stored as a BCrypt hash; changing the
 * environment variable and restarting changes the admin password.
 */
@Configuration
public class AdminInitializer {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    @Bean
    CommandLineRunner createAdmin(
            UserRepository users,
            PasswordEncoder encoder,
            @Value("${bookly.admin.email:}") String email,
            @Value("${bookly.admin.password:}") String password,
            @Value("${bookly.admin.name:Bookly Admin}") String name) {

        return args -> {
            if (email == null || email.isBlank() || password == null || password.isBlank()) {
                log.warn("BOOKLY_ADMIN_EMAIL / BOOKLY_ADMIN_PASSWORD are not set - no administrator account was created or updated.");
                return;
            }
            if (password.length() < 8) {
                log.warn("BOOKLY_ADMIN_PASSWORD must be at least 8 characters - administrator account not changed.");
                return;
            }

            String normalized = email.trim().toLowerCase(Locale.ROOT);
            AppUser admin = users.findByEmailIgnoreCase(normalized).orElse(null);

            if (admin == null) {
                admin = new AppUser();
                admin.setName(name);
                admin.setEmail(normalized);
                admin.setPassword(encoder.encode(password));
                admin.setRole(Role.ADMIN);
                admin.setEnabled(true);
                users.save(admin);
                log.info("Administrator account created for {}", normalized);
            } else if (admin.getRole() != Role.ADMIN) {
                throw new IllegalStateException("Configured admin email belongs to a customer account.");
            } else if (!encoder.matches(password, admin.getPassword()) || !admin.isEnabled()) {
                admin.setPassword(encoder.encode(password));
                admin.setEnabled(true);
                users.save(admin);
                log.info("Administrator password updated from configuration for {}", normalized);
            }
        };
    }
}
