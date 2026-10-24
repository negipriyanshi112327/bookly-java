package com.example.demo.dto;

import java.time.LocalDateTime;

/** What the admin sees about a customer. Deliberately has no password field. */
public final class CustomerSummary {

    private final Long id;
    private final String name;
    private final String email;
    private final boolean enabled;
    private final LocalDateTime createdAt;
    private final long orderCount;

    public CustomerSummary(Long id, String name, String email, boolean enabled, LocalDateTime createdAt, long orderCount) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.orderCount = orderCount;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public boolean isEnabled() { return enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public long getOrderCount() { return orderCount; }

}
