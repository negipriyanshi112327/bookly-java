package com.example.demo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ReviewForm {

    @NotNull(message = "Please choose a rating.")
    @Min(value = 1, message = "Rating must be between 1 and 5 stars.")
    @Max(value = 5, message = "Rating must be between 1 and 5 stars.")
    private Integer rating;

    @NotBlank(message = "Please write a few words about the book.")
    @Size(min = 10, max = 1000, message = "Review must be between 10 and 1000 characters.")
    private String comment;

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
