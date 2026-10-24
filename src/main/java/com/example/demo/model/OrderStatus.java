package com.example.demo.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Order life cycle and the only transitions allowed between statuses.
 *
 * PENDING -> CONFIRMED -> SHIPPED -> DELIVERED
 * PENDING / CONFIRMED -> CANCELLED
 */
public enum OrderStatus {
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    SHIPPED("Shipped"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Enum name, e.g. "PENDING" (used in CSS classes and form values). */
    public String getKey() {
        return name();
    }

    /** Position in the delivery tracker: 0 = Pending ... 3 = Delivered. */
    public int getStep() {
        return ordinal();
    }

    public boolean isCancelled() {
        return this == CANCELLED;
    }

    public boolean isShipped() {
        return this == SHIPPED;
    }

    public Set<OrderStatus> getNextStatuses() {
        return switch (this) {
            case PENDING -> EnumSet.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> EnumSet.of(SHIPPED, CANCELLED);
            case SHIPPED -> EnumSet.of(DELIVERED);
            case DELIVERED, CANCELLED -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    public boolean canTransitionTo(OrderStatus next) {
        return next != null && getNextStatuses().contains(next);
    }

    /** Customers may cancel only before the parcel ships. */
    public boolean isCancellableByCustomer() {
        return this == PENDING || this == CONFIRMED;
    }

    public boolean isFinal() {
        return this == DELIVERED || this == CANCELLED;
    }
}
