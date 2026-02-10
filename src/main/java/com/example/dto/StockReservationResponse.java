package com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservationResponse {
    private Long id;
    private String reservationId;
    private Long productId;
    private Long orderId;
    private Integer quantity;
    private String status; // ACTIVE, CONFIRMED, RELEASED, EXPIRED
    private LocalDateTime reservedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime releasedAt;
}