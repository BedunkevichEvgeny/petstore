package com.chtrembl.petstore.pureorderitemsreserver.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse implements Serializable {
    private String sessionId;
    private String status;
    private String message;

    public OrderResponse(String sessionId) {
        this.sessionId = sessionId;
        this.status = "SUCCESS";
        this.message = "Order items reserved successfully";
    }

    public boolean isSuccess() {
        return "SUCCESS".equals(status);
    }

    public boolean isFailure() {
        return !isSuccess();
    }
}
