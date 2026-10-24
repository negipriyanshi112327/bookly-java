package com.example.demo.security;

import com.example.demo.model.AppUser;
import com.example.demo.model.Role;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * The logged-in user as Spring Security sees it. Stored in the HTTP session,
 * so it holds only small, non-sensitive values (the password hash is erased after login).
 */
public class BooklyUserDetails implements UserDetails, CredentialsContainer {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String name;
    private final String email;
    private final Role role;
    private final boolean enabled;
    private String password;

    public BooklyUserDetails(AppUser user) {
        this(user.getId(), user.getName(), user.getEmail(), user.getPassword(), user.getRole(), user.isEnabled());
    }

    public BooklyUserDetails(Long id, String name, String email, String password, Role role, boolean enabled) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.enabled = enabled;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() { return password; }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public void eraseCredentials() { this.password = null; }
}
