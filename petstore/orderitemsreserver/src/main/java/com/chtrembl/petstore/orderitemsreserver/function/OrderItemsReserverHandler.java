package com.chtrembl.petstore.orderitemsreserver.function;

import com.chtrembl.petstore.orderitemsreserver.model.OrderRequest;
import com.chtrembl.petstore.orderitemsreserver.model.OrderResponse;
import com.chtrembl.petstore.orderitemsreserver.service.OrderItemsReserverService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Azure Function that reserves order items by uploading JSON to Azure Blob Storage.
 * This function receives order details (sessionId, username, userId, products)
 * and stores them as JSON files in Azure Blob Storage.
 */
@Component
@Slf4j
public class OrderItemsReserverHandler {

    @Autowired
    @Lazy
    private OrderItemsReserverService orderItemsReserverService;
    @Autowired
    private ObjectMapper objectMapper;

    @FunctionName("reserveOrderItems")
    public HttpResponseMessage execute(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    route = "orderitems/reserve",
                    authLevel = AuthorizationLevel.ANONYMOUS
            ) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Processing order items reservation request");

        try {
            // Parse request body
            String requestBody = request.getBody().orElse(null);
            if (requestBody == null || requestBody.isEmpty()) {
                log.warn("Empty request body received");
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body(new OrderResponse(null, "ERROR", "Request body is required"))
                        .header("Content-Type", "application/json")
                        .build();
            }

            // Deserialize to OrderRequest
            OrderRequest orderRequest = objectMapper.readValue(requestBody, OrderRequest.class);

            // Validate required fields
            if (orderRequest.getSessionId() == null || orderRequest.getSessionId().isEmpty()) {
                log.warn("Session ID is missing");
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body(new OrderResponse(null, "ERROR", "Session ID is required"))
                        .header("Content-Type", "application/json")
                        .build();
            }

            if (orderRequest.getProducts() == null || orderRequest.getProducts().isEmpty()) {
                log.warn("Product list is empty");
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body(new OrderResponse(orderRequest.getSessionId(), "ERROR", "Product list cannot be empty"))
                        .header("Content-Type", "application/json")
                        .build();
            }

            context.getLogger().info("Processing order reservation for session: " + orderRequest.getSessionId());

            OrderResponse response = orderItemsReserverService.reserveOrderItems(orderRequest);

            context.getLogger().info("Order reservation completed for session: " + orderRequest.getSessionId());

            // Determine HTTP status based on response status
            HttpStatus httpStatus = "SUCCESS".equals(response.getStatus()) ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;

            return request.createResponseBuilder(httpStatus)
                    .body(response)
                    .header("Content-Type", "application/json")
                    .build();

        } catch (Exception e) {
            log.error("Failed to reserve order items", e);
            context.getLogger().severe("Error processing order reservation: " + e.getMessage());

            OrderResponse errorResponse = new OrderResponse(
                    null,
                    "ERROR",
                    "Failed to reserve order items: " + e.getMessage()
            );

            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse)
                    .header("Content-Type", "application/json")
                    .build();
        }
    }
}
