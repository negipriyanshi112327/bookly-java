package com.example.demo.model;

/** Only Cash on Delivery is implemented. No online payment gateway is integrated. */
public enum PaymentMethod {
    COD("Cash on Delivery");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String getKey() {
        return name();
    }
}
