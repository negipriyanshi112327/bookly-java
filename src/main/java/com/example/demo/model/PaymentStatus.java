package com.example.demo.model;

/**
 * For COD: PENDING until the order is delivered (cash collected -> PAID),
 * CANCELLED if the order is cancelled before delivery.
 */
public enum PaymentStatus {
    PENDING("Pay on delivery"),
    PAID("Paid"),
    CANCELLED("Not charged");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
