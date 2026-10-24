package com.example.demo.dto;

import com.example.demo.model.Book;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/** Admin add/edit book form. */
public class BookForm {

    private Long id;

    @NotBlank(message = "Title is required.")
    @Size(max = 200, message = "Title must be at most 200 characters.")
    private String name;

    @NotBlank(message = "Author is required.")
    @Size(max = 150, message = "Author must be at most 150 characters.")
    private String author;

    @Size(max = 2000, message = "Description must be at most 2000 characters.")
    private String description;

    @NotBlank(message = "Category is required.")
    @Size(max = 60, message = "Category must be at most 60 characters.")
    private String category;

    @NotNull(message = "Price is required.")
    @DecimalMin(value = "1.00", message = "Price must be at least ₹1.")
    @DecimalMax(value = "100000.00", message = "Price must be at most ₹1,00,000.")
    @Digits(integer = 6, fraction = 2, message = "Price can have at most 2 decimal places.")
    private BigDecimal price;

    @Size(max = 500, message = "Image URL must be at most 500 characters.")
    @Pattern(regexp = "^$|^(https?://|/).+", message = "Image must be a full http(s) URL or a path starting with /.")
    private String image;

    @NotNull(message = "Stock quantity is required.")
    @Min(value = 0, message = "Stock cannot be negative.")
    @Max(value = 100000, message = "Stock must be at most 100000.")
    private Integer stockQuantity = 0;

    /** Stock shown when the edit form was opened (detects concurrent changes). */
    private Integer originalStock;

    private boolean featured;

    public static BookForm from(Book book) {
        BookForm form = new BookForm();
        form.id = book.getId();
        form.name = book.getName();
        form.author = book.getAuthor();
        form.description = book.getDescription();
        form.category = book.getCategory();
        form.price = book.getPrice();
        form.image = book.getImage();
        form.stockQuantity = book.getStockQuantity();
        form.originalStock = book.getStockQuantity();
        form.featured = book.isFeatured();
        return form;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    public Integer getOriginalStock() { return originalStock; }
    public void setOriginalStock(Integer originalStock) { this.originalStock = originalStock; }
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
}
