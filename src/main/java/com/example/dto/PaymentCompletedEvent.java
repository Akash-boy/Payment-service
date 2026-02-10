package com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {
    private Long paymentId;
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    private String status; // "SUCCESS" or "FAILED"
    private String reservationId; // For inventory to confirm stock
    private String transactionId;
    private LocalDateTime completedAt;
    private String eventType; // "PAYMENT_COMPLETED"
}