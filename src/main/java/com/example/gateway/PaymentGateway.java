package com.example.gateway;

import com.example.dto.PaymentRequest;

import java.math.BigDecimal;

public interface PaymentGateway {

    /**
     * Process payment through the gateway
     * @param request Payment request details
     * @return Payment gateway response
     */
    PaymentGatewayResponse processPayment(PaymentRequest request);

    /**
     * Verify payment status from gateway
     * @param transactionId Gateway transaction ID
     * @return Payment status
     */
    PaymentGatewayResponse verifyPayment(String transactionId);

    /**
     * Initiate refund
     * @param transactionId Original transaction ID
     * @param amount Amount to refund
     * @return Refund response
     */
    PaymentGatewayResponse refundPayment(String transactionId, BigDecimal amount);

    /**
     * Get gateway name
     * @return Gateway identifier
     */
    String getGatewayName();
}