package com.example.demo.dto;

import java.math.BigDecimal;
import java.util.List;

public final class CartView {

    private final List<CartLine> lines;
    private final BigDecimal total;
    private final int totalItems;
    private final List<String> notices;

    public CartView(List<CartLine> lines, BigDecimal total, int totalItems, List<String> notices) {
        this.lines = lines;
        this.total = total;
        this.totalItems = totalItems;
        this.notices = notices;
    }

    public List<CartLine> getLines() { return lines; }
    public BigDecimal getTotal() { return total; }
    public int getTotalItems() { return totalItems; }
    public List<String> getNotices() { return notices; }


    public boolean isEmpty() {
        return lines.isEmpty();
    }

    /** Checkout is allowed only when every line can be fulfilled. */
    public boolean isCheckoutReady() {
        return !lines.isEmpty() && lines.stream().noneMatch(CartLine::hasIssue);
    }
}
