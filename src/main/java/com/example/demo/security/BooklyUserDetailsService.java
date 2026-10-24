package com.example.demo.security;

import com.example.demo.model.Role;
import com.example.demo.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Loads users for ONE role. The customer login form uses the CUSTOMER instance and the
 * admin login form uses the ADMIN instance, so an admin cannot sign in on the customer
 * form and a customer cannot sign in on the admin form.
 */
public class BooklyUserDetailsService implements UserDetailsService {

    private final UserRepository users;
    private final Role requiredRole;

    public BooklyUserDetailsService(UserRepository users, Role requiredRole) {
        this.users = users;
        this.requiredRole = requiredRole;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalized = email == null ? "" : email.trim();
        return users.findByEmailIgnoreCase(normalized)
                .filter(user -> user.getRole() == requiredRole)
                .map(BooklyUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
    }
}
