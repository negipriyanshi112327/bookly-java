package com.example.demo.exception;

/** The requested record does not exist, or belongs to someone else (we do not reveal which). */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
