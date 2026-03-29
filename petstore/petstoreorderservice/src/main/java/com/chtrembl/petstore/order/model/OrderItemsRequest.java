package com.chtrembl.petstore.order.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Request DTO for OrderItemsReserver Azure Function.
 * Represents order reservation data sent to blob storage.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemsRequest {
    private String sessionId;
    private String username;
    private String userId;
    private List<OrderProduct> products;

    /**
     * Create request from Order
     */
    public static OrderItemsRequest fromOrder(Order order) {
        OrderItemsRequest request = new OrderItemsRequest();
        request.setSessionId(order.getId());
        request.setUsername(order.getEmail() != null ? order.getEmail().split("@")[0] : "Guest");
        request.setUserId(order.getEmail());

        // Convert Product list to OrderProduct list
        if (order.getProducts() != null) {
            List<OrderProduct> orderProducts = order.getProducts().stream()
                .map(p -> new OrderProduct(
                    p.getId() != null ? p.getId().toString() : null,
                    p.getName(),
                    p.getQuantity(),
                    null
                ))
                .collect(Collectors.toList());
            request.setProducts(orderProducts);
        }

        return request;
    }

    /**
     * Simplified product model for order items reservation
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderProduct {
        private String id;
        private String name;
        private Integer quantity;
        private Double price;
    }
}
