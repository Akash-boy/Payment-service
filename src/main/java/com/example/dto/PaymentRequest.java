package com.example.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotNull(message = "Order ID is required")
    @Positive(message = "Order ID must be positive")
    private Long orderId;

    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be positive")
    private Long userId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Invalid amount format")
    private BigDecimal amount;

    @NotBlank(message = "Payment method is required")
    @Pattern(regexp = "CREDIT_CARD|DEBIT_CARD|UPI|NET_BANKING|WALLET",
            message = "Invalid payment method")
    private String paymentMethod;

    // Idempotency key to prevent duplicate payments
    @NotBlank(message = "Idempotency key is required")
    @Size(min = 10, max = 100, message = "Idempotency key must be between 10 and 100 characters")
    private String idempotencyKey;

    // Optional: Payment gateway specific details
    private String paymentGatewayReference;
    private String reservationId; // For linking with inventory reservation
}