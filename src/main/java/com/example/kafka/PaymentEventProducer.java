package com.example.kafka;

import com.example.dto.PaymentCompletedEvent;
import com.example.dto.PaymentInitiatedEvent;
import com.example.dto.RefundProcessedEvent;
import com.example.entities.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
@Component
@Slf4j
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String paymentEventsTopic;

    public PaymentEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${kafka.topic.payment-events:payment-events}") String paymentEventsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.paymentEventsTopic = paymentEventsTopic;
    }

    public void publishPaymentInitiated(Payment payment) {
        log.info("Publishing PaymentInitiated event for payment: {}", payment.getId());

        PaymentInitiatedEvent event = PaymentInitiatedEvent.builder()
                .paymentId(payment.getId())
                .reservationId(payment.getReservationId()) // ✅ now String → String, no type mismatch
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .initiatedAt(LocalDateTime.now())
                .eventType("PAYMENT_INITIATED")
                .build();

        kafkaTemplate.send(paymentEventsTopic, String.valueOf(payment.getOrderId()), event); // ✅ use orderId as key
        log.info("PaymentInitiated event published for payment: {}", payment.getId());
    }

    public void publishPaymentCompleted(Payment payment, String reservationId) {
        log.info("Publishing PaymentCompleted event for payment: {}", payment.getId());

        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .status(payment.getStatus().toString())
                .reservationId(reservationId)
                .transactionId(payment.getTransactionId())
                .completedAt(payment.getCompletedAt())
                .eventType("PAYMENT_COMPLETED")
                .failureReason(payment.getFailureReason()) // ✅ include failure reason if any
                .build();

        kafkaTemplate.send(paymentEventsTopic, String.valueOf(payment.getOrderId()), event);
        log.info("PaymentCompleted event published for order: {}", payment.getOrderId());
    }

    public void publishRefundProcessed(Payment payment) {
        log.info("Publishing RefundProcessed event for payment: {}", payment.getId());

        // ✅ Build a proper object — never String.format JSON
        RefundProcessedEvent event = RefundProcessedEvent.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .eventType("REFUND_PROCESSED")
                .processedAt(LocalDateTime.now())
                .build();

        kafkaTemplate.send(paymentEventsTopic, String.valueOf(payment.getOrderId()), event);
        log.info("RefundProcessed event published for payment: {}", payment.getId());
    }
}