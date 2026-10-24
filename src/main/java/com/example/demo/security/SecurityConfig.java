package com.example.demo.security;

import com.example.demo.model.Role;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.authentication.ExceptionMappingAuthenticationFailureHandler;

import java.util.Map;

/**
 * Two filter chains share one HTTP session:
 *  1. /admin/**  - admin login form, every page needs ROLE_ADMIN
 *  2. everything else - customer login form, shop pages
 * Roles are checked on the server for every request, so changing a URL or a session
 * value cannot give a customer admin access or an admin customer-only functions.
 * CSRF protection stays ON; Thymeleaf adds the token to every th:action form.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain adminChain(HttpSecurity http, UserRepository users, PasswordEncoder encoder) throws Exception {
        http.securityMatcher("/admin/**")
                .authenticationManager(authenticationManager(users, encoder, Role.ADMIN))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
                        .requestMatchers("/admin/login").permitAll()
                        .anyRequest().hasRole("ADMIN"))
                .formLogin(form -> form
                        .loginPage("/admin/login")
                        .loginProcessingUrl("/admin/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/admin/dashboard", true)
                        .failureHandler(failureHandler("/admin/login"))
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/admin/logout")
                        .logoutSuccessUrl("/admin/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(redirectTo("/admin/login?required"))
                        .accessDeniedPage("/access-denied"));
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain customerChain(HttpSecurity http, UserRepository users, PasswordEncoder encoder) throws Exception {
        http.authenticationManager(authenticationManager(users, encoder, Role.CUSTOMER))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico", "/error", "/access-denied").permitAll()
                        // customer-only features
                        .requestMatchers("/checkout", "/checkout/**", "/orders", "/orders/**",
                                "/wishlist", "/wishlist/**").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/books/**").hasRole("CUSTOMER")
                        // guests and customers may use the cart, admins may not
                        .requestMatchers("/cart", "/cart/**").access(new WebExpressionAuthorizationManager("!hasRole('ADMIN')"))
                        // public pages
                        .requestMatchers(HttpMethod.GET, "/", "/books/**", "/book-compass").permitAll()
                        .requestMatchers("/login", "/register").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/", false)
                        .failureHandler(failureHandler("/login"))
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(redirectTo("/login?required"))
                        .accessDeniedPage("/access-denied"));
        return http.build();
    }

    /** BCrypt check against users of one role only. */
    private AuthenticationManager authenticationManager(UserRepository users, PasswordEncoder encoder, Role role) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(new BooklyUserDetailsService(users, role));
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    /** Wrong details -> ?error, deactivated account -> ?disabled. */
    private ExceptionMappingAuthenticationFailureHandler failureHandler(String loginPage) {
        ExceptionMappingAuthenticationFailureHandler handler = new ExceptionMappingAuthenticationFailureHandler();
        handler.setDefaultFailureUrl(loginPage + "?error");
        handler.setExceptionMappings(Map.of(DisabledException.class.getName(), loginPage + "?disabled"));
        return handler;
    }

    /** Sends anonymous users to the login page with a "please log in" message. */
    private AuthenticationEntryPoint redirectTo(String url) {
        return (request, response, authException) -> response.sendRedirect(request.getContextPath() + url);
    }
}
