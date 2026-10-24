package com.example.demo.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_orders", indexes = {
        @Index(name = "idx_orders_user", columnList = "user_id"),
        @Index(name = "idx_orders_status", columnList = "status"),
        @Index(name = "idx_orders_date", columnList = "order_date")
})
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The customer who placed the order (nullable only for very old rows). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(length = 100)
    private String customerName;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String address;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod = PaymentMethod.COD;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "payment_status", length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "order_date")
    private LocalDateTime orderDate = LocalDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** One-time token from the checkout form. Unique, so a double-submitted form cannot create two orders. */
    @Column(name = "checkout_token", unique = true, length = 64)
    private String checkoutToken;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();

    public CustomerOrder() {}

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public int getItemCount() {
        return items.stream().mapToInt(OrderItem::getQuantity).sum();
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public String getCustomerName() { return customerName; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public OrderStatus getStatus() { return status; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public LocalDateTime getOrderDate() { return orderDate; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getCheckoutToken() { return checkoutToken; }
    public List<OrderItem> getItems() { return items; }

    public void setUser(AppUser user) { this.user = user; }
    public void setCustomerName(String v) { customerName = v; }
    public void setPhone(String v) { phone = v; }
    public void setAddress(String v) { address = v; }
    public void setTotalAmount(BigDecimal v) { totalAmount = v; }
    public void setStatus(OrderStatus v) { status = v; }
    public void setPaymentMethod(PaymentMethod v) { paymentMethod = v; }
    public void setPaymentStatus(PaymentStatus v) { paymentStatus = v; }
    public void setOrderDate(LocalDateTime v) { orderDate = v; }
    public void setCheckoutToken(String v) { checkoutToken = v; }
}
