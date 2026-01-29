package com.example.controller;

import com.example.dto.PaymentRequest;
import com.example.dto.RefundRequest;
import com.example.entities.Payment;
import com.example.service.PaymentService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@AllArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Process a new payment
     */
    @PostMapping
    public ResponseEntity<Payment> makePayment(@Valid @RequestBody PaymentRequest request) {
        log.info("Received payment request for order: {}", request.getOrderId());
        Payment payment = paymentService.processPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    /**
     * Get payment by order ID
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<Payment> getPaymentByOrder(@PathVariable("orderId") Long orderId) {
        log.info("Fetching payment for order ID: {}", orderId);
        return paymentService.getPaymentByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get payments by user ID with pagination
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<Payment>> getPaymentsByUser(
            @PathVariable("userId") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        log.info("Fetching payments for user ID: {}, page: {}, size: {}", userId, page, size);

        Sort sort = sortDirection.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Payment> payments = paymentService.getPaymentsByUserId(userId, pageable);

        if (payments.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(payments);
    }

    /**
     * Get payment by payment ID
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable("paymentId") Long paymentId) {
        log.info("Fetching payment by ID: {}", paymentId);
        return paymentService.getPaymentById(paymentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Refund a payment
     */
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<Payment> refundPayment(
            @PathVariable("paymentId") Long paymentId,
            @Valid @RequestBody RefundRequest refundRequest) {

        log.info("Processing refund for payment ID: {}, amount: {}",
                paymentId, refundRequest.getAmount());

        Payment payment = paymentService.refundPayment(paymentId, refundRequest.getAmount());
        return ResponseEntity.ok(payment);
    }
}