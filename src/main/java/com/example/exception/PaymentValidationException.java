package com.example.exception;

public class PaymentValidationException extends PaymentException {
    public PaymentValidationException(String message) {
        super(message);
    }
}