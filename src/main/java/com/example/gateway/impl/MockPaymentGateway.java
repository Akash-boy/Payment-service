package com.example.gateway.impl;

import com.example.dto.PaymentRequest;
import com.example.gateway.PaymentGateway;
import com.example.gateway.PaymentGatewayResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@Slf4j
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public PaymentGatewayResponse processPayment(PaymentRequest request) {
        log.info("Processing payment through Mock Gateway for order: {}", request.getOrderId());

        // Simulate API call delay
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate 85% success rate
        boolean isSuccess = Math.random() > 0.15;

        if (isSuccess) {
            return PaymentGatewayResponse.builder()
                    .success(true)
                    .transactionId(UUID.randomUUID().toString())
                    .gatewayReference("MOCK-" + System.currentTimeMillis())
                    .status("SUCCESS")
                    .message("Payment processed successfully")
                    .build();
        } else {
            return PaymentGatewayResponse.builder()
                    .success(false)
                    .status("FAILED")
                    .message("Insufficient funds")
                    .errorCode("INSUFFICIENT_FUNDS")
                    .build();
        }
    }

    @Override
    public PaymentGatewayResponse verifyPayment(String transactionId) {
        log.info("Verifying payment with transaction ID: {}", transactionId);

        return PaymentGatewayResponse.builder()
                .success(true)
                .transactionId(transactionId)
                .status("SUCCESS")
                .message("Payment verified")
                .build();
    }

    @Override
    public PaymentGatewayResponse refundPayment(String transactionId, BigDecimal amount) {
        log.info("Processing refund for transaction: {}, amount: {}", transactionId, amount);

        return PaymentGatewayResponse.builder()
                .success(true)
                .transactionId(UUID.randomUUID().toString())
                .status("REFUNDED")
                .message("Refund processed successfully")
                .build();
    }

    @Override
    public String getGatewayName() {
        return "MOCK_GATEWAY";
    }
}