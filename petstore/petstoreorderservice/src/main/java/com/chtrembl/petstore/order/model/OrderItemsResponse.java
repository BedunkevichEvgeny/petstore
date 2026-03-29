package com.chtrembl.petstore.order.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO from OrderItemsReserver Azure Function.
 * Indicates success or failure of order items reservation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemsResponse {
    private String sessionId;
    private String status;
    private String message;

    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(status);
    }
}
