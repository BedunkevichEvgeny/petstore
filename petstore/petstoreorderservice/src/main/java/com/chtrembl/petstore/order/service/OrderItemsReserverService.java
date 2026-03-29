package com.chtrembl.petstore.order.service;

import com.chtrembl.petstore.order.model.OrderItemsRequest;
import com.chtrembl.petstore.order.model.OrderItemsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service for OrderItemsReserver Azure Function HTTP communication.
 * Handles REST calls using RestTemplate for order items reservation.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderItemsReserverService {

    private final RestTemplate restTemplate;

    @Value("${petstore.service.orderitemsreserver.url}")
    private String orderItemsReserverUrl;

    /**
     * Reserve order items by uploading to Azure Blob Storage.
     *
     * @param request Order items reservation request with sessionId, username, and products
     * @return Response indicating success or failure with sessionId
     */
    public OrderItemsResponse reserveOrderItems(OrderItemsRequest request) {
        String url = orderItemsReserverUrl + "/api/orderitems/reserve";

        log.debug("Calling OrderItemsReserver at URL: {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<OrderItemsRequest> httpEntity = new HttpEntity<>(request, headers);

        ResponseEntity<OrderItemsResponse> response = restTemplate.postForEntity(
                url,
                httpEntity,
                OrderItemsResponse.class
        );

        return response.getBody();
    }
}
