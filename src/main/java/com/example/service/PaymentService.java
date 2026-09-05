package com.example.service;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    private final CacheManager cacheManager;

    /**
     * Step 1 of saga — triggered by STOCK_RESERVED event
     * Creates PENDING payment and publishes PAYMENT_INITIATED
     */
    @Transactional
    public Payment initiatePaymentForOrder(Long orderId, String reservationId) {
        log.info("Auto-initiating payment for order: {} with reservation: {}", orderId, reservationId);

        return paymentRepository.findByOrderId(orderId)
                .map(existing -> {
                    log.warn("Payment already exists for order: {}. Status: {}",
                            orderId, existing.getStatus());
                    return existing;
                })
                .orElseGet(() -> {
                    // 1. Fetch order details
                    OrderDetailsResponse orderDetails = orderServiceClient.getOrderDetails(orderId);

                    // 2. Validate stock is still active
                    validateStockStatus(orderId);

                    // 3. Idempotency key
                    String idempotencyKey = UUID.randomUUID().toString();

                    // 4. Check duplicate
                    paymentRepository.findByIdempotencyKey(idempotencyKey)
                            .ifPresent(p -> {
                                throw new DuplicatePaymentException("Payment already processed", p.getId());
                            });

                    validateAmount(orderDetails.getTotalAmount());

                    // 5. Save PENDING payment
                    Payment payment = Payment.builder()
                            .orderId(orderId)
                            .userId(orderDetails.getUserId())
                            .amount(orderDetails.getTotalAmount())
                            .paymentMethod("CREDIT_CARD")
                            .status(PaymentStatus.PENDING)
                            .idempotencyKey(idempotencyKey)
                            .reservationId(reservationId)   // ✅ saved to entity
                            .retryCount(0)
                            .build();

                    payment = paymentRepository.save(payment);

                    // 6. Publish PAYMENT_INITIATED — gateway NOT called yet
                    paymentEventProducer.publishPaymentInitiated(payment);
                    log.info("PaymentInitiated published for order: {}", orderId);

                    return payment;
                });
    }

    /**
     * Step 2 of saga — triggered by STOCK_CONFIRMED event
     * Calls gateway and publishes PAYMENT_COMPLETED
     */
    public void completePayment(Long orderId, String reservationId) {
        log.info("Completing payment for order: {} after stock confirmed", orderId);

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentException("Payment not found for order: " + orderId));


        // Evict now that we know the paymentId — before any early returns
        cacheManager.getCache("paymentsById").evict(payment.getId());
        cacheManager.getCache("paymentsByOrderId").evict(orderId);

        // Idempotency guard
        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.warn("Payment for order: {} already in status: {}, skipping",
                    orderId, payment.getStatus());
            return;
        }

        try {
            PaymentRequest request = PaymentRequest.builder()
                    .orderId(payment.getOrderId())
                    .userId(payment.getUserId())
                    .amount(payment.getAmount())
                    .paymentMethod(payment.getPaymentMethod())
                    .idempotencyKey(payment.getIdempotencyKey())
                    .reservationId(reservationId)
                    .build();

            PaymentGatewayResponse gatewayResponse = paymentGateway.processPayment(request);

            if (gatewayResponse.isSuccess()) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setTransactionId(gatewayResponse.getTransactionId());
                payment.setGatewayReference(gatewayResponse.getGatewayReference());
                payment.setCompletedAt(LocalDateTime.now());
                log.info("✅ Payment successful for order: {}", orderId);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason(gatewayResponse.getMessage());
                payment.setCompletedAt(LocalDateTime.now());
                log.error("❌ Payment failed for order: {}, reason: {}",
                        orderId, gatewayResponse.getMessage());
            }

        } catch (Exception e) {
            log.error("Technical error completing payment for order: {}", orderId, e);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Technical Error: " + e.getMessage());
            payment.setCompletedAt(LocalDateTime.now());
        }

        // Save final status
        payment = paymentRepository.save(payment);

        // Publish PAYMENT_COMPLETED — order service marks order complete
        paymentEventProducer.publishPaymentCompleted(payment, reservationId);
        log.info("PaymentCompleted published for order: {}", orderId);
    }

    // --- Supporting methods ---

    private void validateStockStatus(Long orderId) {
        try {
            StockReservationResponse reservation =
                    inventoryServiceClient.getReservationByOrderId(orderId);
            if (!"ACTIVE".equals(reservation.getStatus())) {
                throw new PaymentValidationException(
                        "Inventory reservation is not ACTIVE. Current status: "
                                + reservation.getStatus());
            }
        } catch (PaymentValidationException e) {
            throw e; // rethrow validation errors
        } catch (Exception e) {
            log.warn("Could not verify reservation for order {}, proceeding with caution", orderId);
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentValidationException("Payment amount must be positive");
        }
    }

    // --- Read Operations ---
@Cacheable(value = "payments", key = "#p0")
    public Optional<Payment> getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    public Page<Payment> getPaymentsByUserId(Long userId, Pageable pageable) {
        return paymentRepository.findByUserId(userId, pageable);
    }
    @Cacheable(value = "payments", key = "#p0")
    public Optional<Payment> getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId);
    }

    public Payment refundPayment(Long paymentId, BigDecimal amount) throws Exception {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new Exception("Payment not found with ID: " + paymentId));

        PaymentGatewayResponse refundResponse =
                paymentGateway.refundPayment(payment.getTransactionId(), amount);

        if (refundResponse.isSuccess()) {
            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
            log.info("Payment refunded: PaymentId={}, Amount={}", paymentId, amount);

            cacheManager.getCache("paymentsById").evict(paymentId);
            cacheManager.getCache("paymentsByOrderId").evict(payment.getOrderId());
        } else {
            log.error("Refund failed: PaymentId={}, Reason={}", paymentId, refundResponse.getMessage());
            throw new Exception("Refund failed: " + refundResponse.getMessage());
        }
        return payment;
    }
}