package com.chtrembl.petstore.orderitemsreserver.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderData implements Serializable {
    private String sessionId;
    private String username;
    private String userId;
    private List<Product> products;
    private LocalDateTime timestamp;
    private String status;

    public OrderData(OrderRequest request) {
        this.sessionId = request.getSessionId();
        this.username = request.getUsername();
        this.userId = request.getUserId();
        this.products = request.getProducts();
        this.timestamp = LocalDateTime.now();
        this.status = "RESERVED";
    }
}
