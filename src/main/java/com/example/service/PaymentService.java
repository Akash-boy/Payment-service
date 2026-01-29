package com.example.service;

import com.example.dto.PaymentRequest;
import com.example.entities.Payment;
import com.example.entities.PaymentStatus;
import com.example.eventProducer.PaymentEventProducer;
import com.example.exception.DuplicatePaymentException;
import com.example.exception.PaymentException;
import com.example.exception.PaymentValidationException;
import com.example.gateway.PaymentGateway;
import com.example.gateway.PaymentGatewayResponse;
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

@Service
@AllArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer paymentEventProducer;
    private final PaymentGateway paymentGateway; // Inject your gateway implementation

    /**
     * Process payment with idempotency support
     */
    @Transactional
    public Payment processPayment(PaymentRequest request) {
        log.info("Processing payment for order: {}, idempotency key: {}",
                request.getOrderId(), request.getIdempotencyKey());

        // Check for duplicate payment using idempotency key
        Optional<Payment> existingPayment = paymentRepository
                .findByIdempotencyKeyWithLock(request.getIdempotencyKey());

        if (existingPayment.isPresent()) {
            Payment existing = existingPayment.get();
            log.warn("Duplicate payment attempt detected for idempotency key: {}",
                    request.getIdempotencyKey());
            throw new DuplicatePaymentException(
                    "Payment already processed with this idempotency key",
                    existing.getId()
            );
        }

        // Validate payment request
        validatePaymentRequest(request);

        // Create initial payment record
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
        log.info("Created payment record with ID: {}", payment.getId());

        // Process payment through gateway
        try {
            payment.setStatus(PaymentStatus.PROCESSING);
            payment = paymentRepository.save(payment);

            PaymentGatewayResponse gatewayResponse = paymentGateway.processPayment(request);

            // Update payment based on gateway response
            if (gatewayResponse.isSuccess()) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setTransactionId(gatewayResponse.getTransactionId());
                payment.setGatewayReference(gatewayResponse.getGatewayReference());
                payment.setCompletedAt(LocalDateTime.now());
                log.info("Payment successful for order: {}, transaction ID: {}",
                        request.getOrderId(), gatewayResponse.getTransactionId());
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason(gatewayResponse.getMessage());
                payment.setCompletedAt(LocalDateTime.now());
                log.error("Payment failed for order: {}, reason: {}",
                        request.getOrderId(), gatewayResponse.getMessage());
            }

            payment = paymentRepository.save(payment);

            // Publish event to Kafka
            publishPaymentEvent(payment);

            return payment;

        } catch (Exception e) {
            log.error("Error processing payment for order: {}", request.getOrderId(), e);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Technical error: " + e.getMessage());
            payment.setCompletedAt(LocalDateTime.now());
            payment = paymentRepository.save(payment);

            // Still publish the failed event
            publishPaymentEvent(payment);

            throw new PaymentException("Failed to process payment", e);
        }
    }

    /**
     * Get payment by order ID
     */
    public Optional<Payment> getPaymentByOrderId(Long orderId) {
        log.info("Fetching payment for order ID: {}", orderId);
        return paymentRepository.findByOrderId(orderId);
    }

    /**
     * Get payments by user ID with pagination
     */
    public Page<Payment> getPaymentsByUserId(Long userId, Pageable pageable) {
        log.info("Fetching payments for user ID: {}", userId);
        return paymentRepository.findByUserId(userId, pageable);
    }

    /**
     * Get payment by ID
     */
    public Optional<Payment> getPaymentById(Long paymentId) {
        log.info("Fetching payment by ID: {}", paymentId);
        return paymentRepository.findById(paymentId);
    }

    /**
     * Refund a payment
     */
    @Transactional
    public Payment refundPayment(Long paymentId, BigDecimal refundAmount) {
        log.info("Processing refund for payment ID: {}, amount: {}", paymentId, refundAmount);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException("Payment not found with ID: " + paymentId));

        // Validate refund
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new PaymentValidationException("Can only refund successful payments");
        }

        if (refundAmount.compareTo(payment.getAmount()) > 0) {
            throw new PaymentValidationException("Refund amount cannot exceed payment amount");
        }

        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentValidationException("Refund amount must be positive");
        }

        // Process refund through gateway
        try {
            PaymentGatewayResponse refundResponse = paymentGateway.refundPayment(
                    payment.getTransactionId(),
                    refundAmount
            );

            if (refundResponse.isSuccess()) {
                // Update payment status
                if (refundAmount.compareTo(payment.getAmount()) == 0) {
                    payment.setStatus(PaymentStatus.REFUNDED);
                } else {
                    payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
                }

                payment = paymentRepository.save(payment);
                log.info("Refund successful for payment ID: {}", paymentId);

                // Publish refund event
                publishPaymentEvent(payment);

                return payment;
            } else {
                throw new PaymentException("Refund failed: " + refundResponse.getMessage());
            }

        } catch (Exception e) {
            log.error("Error processing refund for payment ID: {}", paymentId, e);
            throw new PaymentException("Failed to process refund", e);
        }
    }

    /**
     * Validate payment request
     */
    private void validatePaymentRequest(PaymentRequest request) {
        // Additional business validations beyond bean validation
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentValidationException("Payment amount must be positive");
        }

        // You can add more validations like:
        // - Check if order exists (call order service)
        // - Check if order is already paid
        // - Validate user exists
        // - Check amount limits based on payment method

        log.debug("Payment request validation passed for order: {}", request.getOrderId());
    }

    /**
     * Publish payment event to Kafka
     */
    private void publishPaymentEvent(Payment payment) {
        try {
            paymentEventProducer.publishPaymentEvent(payment);
            log.info("Published payment event for payment ID: {}, status: {}",
                    payment.getId(), payment.getStatus());
        } catch (Exception e) {
            // Log error but don't fail the payment
            log.error("Failed to publish payment event for payment ID: {}",
                    payment.getId(), e);
        }
    }
}