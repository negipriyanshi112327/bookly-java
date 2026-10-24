package com.example.demo.exception;

import com.example.demo.model.OrderStatus;

public class InvalidStatusTransitionException extends BusinessException {
    public InvalidStatusTransitionException(OrderStatus from, OrderStatus to) {
        super("An order cannot move from " + from.getLabel() + " to "
                + (to == null ? "an unknown status" : to.getLabel()) + ".");
    }

    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
