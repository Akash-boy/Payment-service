package com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservedEvent {
    private Long orderId;
    private boolean success;
    private String message;
    private String reservationId;
    private String eventType; // "STOCK_RESERVED"
}