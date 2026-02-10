package com.example.client;

import com.example.dto.StockReservationResponse;
import com.example.exception.InventoryServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
@Slf4j
public class InventoryServiceClient {

    private final RestTemplate restTemplate;
    private final String inventoryServiceUrl;

    public InventoryServiceClient(
            RestTemplate restTemplate,
            @Value("${inventory.service.url:http://localhost:8082}") String inventoryServiceUrl) {
        this.restTemplate = restTemplate;
        this.inventoryServiceUrl = inventoryServiceUrl;
    }

    /**
     * Get stock reservation details by order ID
     */
    public StockReservationResponse getReservationByOrderId(Long orderId) {
        try {
            String url = inventoryServiceUrl + "/api/v1/inventory/reservation/order/" + orderId;

            log.info("Fetching reservation details from Inventory Service for order: {}", orderId);

            StockReservationResponse response = restTemplate.getForObject(url, StockReservationResponse.class);

            if (response == null) {
                throw new InventoryServiceException("Reservation not found for order: " + orderId);
            }

            log.info("Reservation found. Reservation ID: {}, Status: {}",
                    response.getReservationId(), response.getStatus());

            return response;

        } catch (Exception e) {
            log.error("Error fetching reservation for order {}: {}", orderId, e.getMessage(), e);
            throw new InventoryServiceException("Failed to fetch reservation: " + e.getMessage(), e);
        }
    }

    /**
     * Get all reservations for an order (in case of multiple items)
     */
    public List<StockReservationResponse> getAllReservationsByOrderId(Long orderId) {
        try {
            String url = inventoryServiceUrl + "/api/v1/inventory/reservations/order/" + orderId;

            log.info("Fetching all reservations for order: {}", orderId);

            ResponseEntity<List<StockReservationResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<StockReservationResponse>>() {}
            );

            List<StockReservationResponse> reservations = response.getBody();

            log.info("Found {} reservations for order: {}",
                    reservations != null ? reservations.size() : 0, orderId);

            return reservations;

        } catch (Exception e) {
            log.error("Error fetching reservations for order {}: {}", orderId, e.getMessage(), e);
            throw new InventoryServiceException("Failed to fetch reservations: " + e.getMessage(), e);
        }
    }
}