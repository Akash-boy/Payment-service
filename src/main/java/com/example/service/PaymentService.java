package com.example.service;

import com.example.client.InventoryServiceClient;
import com.example.client.OrderServiceClient;
import com.example.dto.OrderDetailsResponse;
import com.example.dto.PaymentRequest;
import com.example.dto.StockReservationResponse;
import com.example.entities.Payment;
import com.example.entities.PaymentStatus;
import com.example.exception.*;
import com.example.gateway.PaymentGateway;
import com.example.gateway.PaymentGatewayResponse;
import com.example.kafka.PaymentEventProducer;
import com.example.repository.PaymentRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer paymentEventProducer;
    private final PaymentGateway paymentGateway;
    private final OrderServiceClient orderServiceClient;
    private final InventoryServiceClient inventoryServiceClient;

    /**
     * Automatic Initiation Triggered by Inventory Reservation Event
     */
    @Transactional
    public Payment initiatePaymentForOrder(Long orderId, String reservationId) {
        log.info("Auto-initiating payment for order: {} with reservation: {}", orderId, reservationId);

        // 1. Idempotency Check for the Order
        return paymentRepository.findByOrderId(orderId)
                .map(existing -> {
                    log.warn("Payment already exists for order: {}. Status: {}", orderId, existing.getStatus());
                    return existing;
                })
                .orElseGet(() -> {
                    // 2. Fetch Context from other Microservices
                    OrderDetailsResponse orderDetails = orderServiceClient.getOrderDetails(orderId);
                    validateStockStatus(orderId);

                    // 3. Map to Request DTO for unified processing
                    PaymentRequest request = PaymentRequest.builder()
                            .orderId(orderId)
                            .userId(orderDetails.getUserId())
                            .amount(orderDetails.getTotalAmount())
                            .paymentMethod("CREDIT_CARD") // Default or fetched from Order metadata
                            .idempotencyKey(UUID.randomUUID().toString())
                            .build();

                    return processPayment(request);
                });
    }

    /**
     * Core Payment Logic with Gateway Integration and Idempotency
     */
    @Transactional
    public Payment processPayment(PaymentRequest request) {
        log.info("Processing payment for order: {}, idempotency key: {}",
                request.getOrderId(), request.getIdempotencyKey());

        // Check for duplicate payment using idempotency key with Pessimistic Lock
        paymentRepository.findByIdempotencyKeyWithLock(request.getIdempotencyKey())
                .ifPresent(p -> {
                    throw new DuplicatePaymentException("Payment already processed", p.getId());
                });

        validatePaymentRequest(request);

        // Create initial PENDING record
        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .idempotencyKey(request.getIdempotencyKey())
                .retryCount(0)
                .build();

        payment = paymentRepository.save(payment);

        try {
            payment.setStatus(PaymentStatus.PROCESSING);
            paymentRepository.saveAndFlush(payment);

            // Call external Gateway (Stripe, Razorpay, etc.)
            PaymentGatewayResponse gatewayResponse = paymentGateway.processPayment(request);

            if (gatewayResponse.isSuccess()) {
                handleSuccess(payment, gatewayResponse);
            } else {
                handleFailure(payment, gatewayResponse.getMessage());
            }

        } catch (Exception e) {
            log.error("Technical error processing payment for order: {}", request.getOrderId(), e);
            handleFailure(payment, "Technical Error: " + e.getMessage());
            // Rethrowing is optional depending on whether you want to retry the transaction
            throw new PaymentException("Payment failed due to system error", e);
        }

        publishPaymentEvent(payment, request.getReservationId());
        return payment;
    }

    private void handleSuccess(Payment payment, PaymentGatewayResponse response) {
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionId(response.getTransactionId());
        payment.setGatewayReference(response.getGatewayReference());
        payment.setCompletedAt(LocalDateTime.now());
        paymentRepository.save(payment);
        log.info("✅ Payment Successful: OrderId={}, PaymentId={}", payment.getOrderId(), payment.getId());
    }

    private void handleFailure(Payment payment, String reason) {
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(reason);
        payment.setCompletedAt(LocalDateTime.now());
        paymentRepository.save(payment);
        log.error("❌ Payment Failed for Order {}: {}", payment.getOrderId(), reason);
    }

    private void validateStockStatus(Long orderId) {
        try {
            StockReservationResponse reservation = inventoryServiceClient.getReservationByOrderId(orderId);
            if (!"ACTIVE".equals(reservation.getStatus())) {
                throw new PaymentValidationException("Inventory reservation is not ACTIVE. Current status: " + reservation.getStatus());
            }
        } catch (Exception e) {
            log.warn("Could not verify reservation status for order {}, proceeding with caution", orderId);
        }
    }

    private void validatePaymentRequest(PaymentRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentValidationException("Payment amount must be positive");
        }
    }

    private void publishPaymentEvent(Payment payment, String reservationId) {
        try {
            // Using the specialized producer to notify Inventory and Order services
            paymentEventProducer.publishPaymentCompleted(payment,reservationId);
            log.info("Published Kafka event for payment status: {}", payment.getStatus());
        } catch (Exception e) {
            log.error("Failed to publish Kafka event for payment ID: {}", payment.getId(), e);
        }
    }

    // --- Read Operations ---

    public Optional<Payment> getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    public Page<Payment> getPaymentsByUserId(Long userId, Pageable pageable) {
        return paymentRepository.findByUserId(userId, pageable);
    }
}