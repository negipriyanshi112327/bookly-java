package com.example.demo.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * One line of an order. bookName and unitPrice are snapshots taken at purchase time,
 * so the order stays correct even if the book is renamed, repriced or archived later.
 */
@Entity
@Table(name = "order_items", indexes = {
        @Index(name = "idx_order_items_order", columnList = "order_id"),
        @Index(name = "idx_order_items_book", columnList = "book_id")
})
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;

    /** Link back to the catalogue (used for stock restore and verified-purchase reviews). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private Book book;

    @Column(name = "book_name", length = 200)
    private String bookName;

    private int quantity;

    /** Kept in the existing "price" column. */
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(precision = 12, scale = 2)
    private BigDecimal subtotal;

    public OrderItem() {}

    public OrderItem(Book book, int quantity) {
        this.book = book;
        this.bookName = book.getName();
        this.quantity = quantity;
        this.unitPrice = book.getPrice();
        this.subtotal = book.getPrice().multiply(BigDecimal.valueOf(quantity));
    }

    public Long getId() { return id; }
    public CustomerOrder getOrder() { return order; }
    public Book getBook() { return book; }
    public String getBookName() { return bookName; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }

    public BigDecimal getSubtotal() {
        if (subtotal != null) return subtotal;
        return unitPrice == null ? BigDecimal.ZERO : unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /** Id of the catalogue book without loading it (null for very old rows). */
    public Long getBookId() {
        return book == null ? null : book.getId();
    }

    public void setOrder(CustomerOrder order) { this.order = order; }
}
