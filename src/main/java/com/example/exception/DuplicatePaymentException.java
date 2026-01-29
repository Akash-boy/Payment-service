package com.example.exception;

public class DuplicatePaymentException extends PaymentException {
    private final Long existingPaymentId;

    public DuplicatePaymentException(String message, Long existingPaymentId) {
        super(message);
        this.existingPaymentId = existingPaymentId;
    }

    public Long getExistingPaymentId() {
        return existingPaymentId;
    }
}