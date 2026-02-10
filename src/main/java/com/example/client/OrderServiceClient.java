package com.example.client;

import com.example.dto.OrderDetailsResponse;
import com.example.exception.OrderServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class OrderServiceClient {

    private final RestTemplate restTemplate;
    private final String orderServiceUrl;

    public OrderServiceClient(
            RestTemplate restTemplate,
            @Value("${order.service.url:http://localhost:8081}") String orderServiceUrl) {
        this.restTemplate = restTemplate;
        this.orderServiceUrl = orderServiceUrl;
    }

    /**
     * Get order details by order ID
     */
    public OrderDetailsResponse getOrderDetails(Long orderId) {
        try {
            String url = orderServiceUrl + "/api/v1/orders/" + orderId;

            log.info("Fetching order details from Order Service for order: {}", orderId);

            OrderDetailsResponse response = restTemplate.getForObject(url, OrderDetailsResponse.class);

            if (response == null) {
                throw new OrderServiceException("Order not found with ID: " + orderId);
            }

            log.info("Order details fetched successfully. Order total: {}", response.getTotalAmount());

            return response;

        } catch (Exception e) {
            log.error("Error fetching order details for order {}: {}", orderId, e.getMessage(), e);
            throw new OrderServiceException("Failed to fetch order details: " + e.getMessage(), e);
        }
    }
}