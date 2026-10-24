package com.example.demo.exception;

/** A rule of the shop was broken (e.g. empty cart, invalid quantity). The message is safe to show to users. */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
