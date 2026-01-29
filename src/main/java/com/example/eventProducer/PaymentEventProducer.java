package com.example.eventProducer;

import com.example.entities.Payment;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentEventProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topicName;

    public PaymentEventProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${spring.kafka.topic-json.name}") String topicName) {  // @Value goes here
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topicName = topicName;
    }

    public void publishPaymentEvent(Payment payment) {
        try {
            String eventJson = objectMapper.writeValueAsString(payment);
            kafkaTemplate.send(topicName, eventJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish payment event", e);
        }
    }
}