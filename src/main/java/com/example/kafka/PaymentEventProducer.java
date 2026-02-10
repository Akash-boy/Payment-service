package com.example.kafka;

import com.example.dto.PaymentCompletedEvent;
import com.example.dto.PaymentInitiatedEvent;
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

    /**
     * Publish PaymentInitiated event
     */
    public void publishPaymentInitiated(Payment payment) {
        log.info("Publishing PaymentInitiated event for payment: {}", payment.getId());

        PaymentInitiatedEvent event = PaymentInitiatedEvent.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .initiatedAt(LocalDateTime.now())
                .eventType("PAYMENT_INITIATED")
                .build();

        kafkaTemplate.send(paymentEventsTopic, "PAYMENT_INITIATED", event);

        log.info("PaymentInitiated event published for payment: {}", payment.getId());
    }

    /**
     * Publish PaymentCompleted event
     * This triggers stock confirmation in Inventory Service
     */
    public void publishPaymentCompleted(Payment payment, String reservationId) {
        log.info("Publishing PaymentCompleted event for payment: {}, status: {}",
                payment.getId(), payment.getStatus());

        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .status(payment.getStatus().toString()) // "SUCCESS" or "FAILED"
                .reservationId(reservationId)
                .transactionId(payment.getTransactionId())
                .completedAt(payment.getCompletedAt())
                .eventType("PAYMENT_COMPLETED")
                .build();

        kafkaTemplate.send(paymentEventsTopic, "PAYMENT_COMPLETED", event);

        log.info("PaymentCompleted event published for order: {}", payment.getOrderId());
    }

    /**
     * Publish RefundProcessed event
     */
    public void publishRefundProcessed(Payment payment) {
        log.info("Publishing RefundProcessed event for payment: {}", payment.getId());

        String event = String.format(
                "{\"paymentId\": %d, \"orderId\": %d, \"amount\": %.2f, \"eventType\": \"REFUND_PROCESSED\"}",
                payment.getId(), payment.getOrderId(), payment.getAmount()
        );

        kafkaTemplate.send(paymentEventsTopic, "REFUND_PROCESSED", event);
    }
}