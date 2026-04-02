package com.example.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StockConfirmedEvent {

    private String eventType = "STOCK_CONFIRMED";   // consumer can filter on this

    private Long orderId;
    private Long productId;
    private String reservationId;
    private Integer quantity;
    private LocalDateTime confirmedAt;              // copied FROM the entity after save
}
