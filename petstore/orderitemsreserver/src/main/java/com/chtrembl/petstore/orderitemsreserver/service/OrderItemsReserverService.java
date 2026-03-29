package com.chtrembl.petstore.orderitemsreserver.service;

import com.chtrembl.petstore.orderitemsreserver.model.OrderData;
import com.chtrembl.petstore.orderitemsreserver.model.OrderRequest;
import com.chtrembl.petstore.orderitemsreserver.model.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OrderItemsReserverService {

    @Autowired
    @Lazy
    private BlobStorageService blobStorageService;

    public OrderResponse reserveOrderItems(OrderRequest request) {
        try {
            log.info("Processing order reservation for session: {}", request.getSessionId());

            // Create order data with timestamp and status
            OrderData orderData = new OrderData(request);

            // Upload to blob storage (overwrites if exists)
            blobStorageService.uploadOrderData(request.getSessionId(), orderData);

            log.info("Order items reserved successfully for session: {}", request.getSessionId());
            return new OrderResponse(request.getSessionId());

        } catch (Exception e) {
            log.error("Failed to reserve order items for session: {}", request.getSessionId(), e);
            return new OrderResponse(
                    request.getSessionId(),
                    "ERROR",
                    "Failed to reserve order items: " + e.getMessage()
            );
        }
    }

    public String getOrderData(String sessionId) {
        try {
            return blobStorageService.downloadOrderData(sessionId);
        } catch (Exception e) {
            log.error("Failed to retrieve order data for session: {}", sessionId, e);
            return null;
        }
    }

    public boolean cancelReservation(String sessionId) {
        try {
            return blobStorageService.deleteOrderData(sessionId);
        } catch (Exception e) {
            log.error("Failed to cancel reservation for session: {}", sessionId, e);
            return false;
        }
    }
}
