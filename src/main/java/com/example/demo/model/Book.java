package com.example.demo.model;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A book in the catalogue. Still mapped to the existing "products" table.
 *
 * @DynamicUpdate makes Hibernate write only the columns that changed, so editing a
 * book's title or price never overwrites stock that an order changed at the same time.
 */
@Entity
@DynamicUpdate
@Table(name = "products", indexes = {
        @Index(name = "idx_products_name", columnList = "name"),
        @Index(name = "idx_products_author", columnList = "author"),
        @Index(name = "idx_products_category", columnList = "category")
})
public class Book {

    public static final int LOW_STOCK_LIMIT = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 150)
    private String author;

    @Column(length = 2000)
    private String description;

    @Column(length = 60)
    private String category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 500)
    private String image;

    @ColumnDefault("0")
    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @ColumnDefault("0")
    @Column(nullable = false)
    private boolean featured;

    /** false = archived: hidden from the store but kept because old orders refer to it. */
    @ColumnDefault("1")
    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Book() {}

    public Book(String name, String author, BigDecimal price, String image) {
        this.name = name;
        this.author = author;
        this.price = price;
        this.image = image;
    }

    public boolean isInStock() { return active && stockQuantity > 0; }

    public boolean isLowStock() { return stockQuantity > 0 && stockQuantity <= LOW_STOCK_LIMIT; }

    public String getAvailabilityLabel() {
        if (!active) return "Unavailable";
        if (stockQuantity <= 0) return "Out of stock";
        if (isLowStock()) return "Only " + stockQuantity + " left";
        return "In stock";
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getAuthor() { return author; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public BigDecimal getPrice() { return price; }
    public String getImage() { return image; }
    public int getStockQuantity() { return stockQuantity; }
    public boolean isFeatured() { return featured; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setAuthor(String author) { this.author = author; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(String category) { this.category = category; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setImage(String image) { this.image = image; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    public void setActive(boolean active) { this.active = active; }
}
