package com.example.demo.dto;

import com.example.demo.model.Book;

import java.math.BigDecimal;

/**
 * One cart row, built on the server from the current database price.
 * issue is a message when the line cannot be ordered as it is (e.g. out of stock).
 */
public final class CartLine {

    private final Book book;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal subtotal;
    private final int maxQuantity;
    private final String issue;

    public CartLine(Book book, int quantity, BigDecimal unitPrice, BigDecimal subtotal, int maxQuantity, String issue) {
        this.book = book;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
        this.maxQuantity = maxQuantity;
        this.issue = issue;
    }

    public Book getBook() { return book; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getSubtotal() { return subtotal; }
    public int getMaxQuantity() { return maxQuantity; }
    public String getIssue() { return issue; }


    public boolean hasIssue() {
        return issue != null;
    }
}
