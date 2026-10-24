package com.example.demo.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "wishlists",
        uniqueConstraints = @UniqueConstraint(name = "uk_wishlist_user_book", columnNames = {"user_id", "book_id"}),
        indexes = @Index(name = "idx_wishlists_user", columnList = "user_id"))
public class WishlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public WishlistItem() {}

    public WishlistItem(AppUser user, Book book) {
        this.user = user;
        this.book = book;
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public Book getBook() { return book; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
