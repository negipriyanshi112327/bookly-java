package com.example.demo.dto;

import com.example.demo.model.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Delivery details typed by the customer. Prices and totals are never taken from the form. */
public class CheckoutForm {

    @NotBlank(message = "Please enter the recipient's name.")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters.")
    private String customerName;

    @NotBlank(message = "Please enter a phone number.")
    @Pattern(regexp = "^(\\+91[\\- ]?)?[6-9]\\d{9}$",
            message = "Enter a valid 10-digit Indian mobile number (optionally starting with +91).")
    private String phone;

    @NotBlank(message = "Please enter the delivery address.")
    @Size(min = 10, max = 250, message = "Address must be between 10 and 250 characters.")
    private String address;

    @NotNull(message = "Please choose a payment method.")
    private PaymentMethod paymentMethod = PaymentMethod.COD;

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
}
