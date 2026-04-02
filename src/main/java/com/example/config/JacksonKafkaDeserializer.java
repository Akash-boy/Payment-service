package com.example.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

public class JacksonKafkaDeserializer implements Deserializer<Map<String, Object>> {

    private final ObjectMapper objectMapper;

    public JacksonKafkaDeserializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper
                .copy()
                // ✅ never crash on unknown fields — critical for microservices
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public Map<String, Object> deserialize(String topic, byte[] data) {
        if (data == null) return null;
        try {
            return objectMapper.readValue(data, Map.class);
        } catch (Exception e) {
            throw new SerializationException(
                    "Error deserializing JSON from topic: " + topic, e);
        }
    }
}