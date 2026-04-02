package com.example.config;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;

public class JacksonKafkaSerializer implements Serializer<Object> {
    private final ObjectMapper objectMapper;
    public JacksonKafkaSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    @Override
    public byte[] serialize(String topic, Object data) {
        if (data == null) return null;
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new SerializationException(
                    "Error serializing object to JSON for topic: " + topic, e);
        }
    }
}
