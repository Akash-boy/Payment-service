package com.example.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentGatewayResponse {
    private boolean success;
    private String transactionId;
    private String gatewayReference;
    private String status; // SUCCESS, FAILED, PENDING
    private String message;
    private String errorCode;
}