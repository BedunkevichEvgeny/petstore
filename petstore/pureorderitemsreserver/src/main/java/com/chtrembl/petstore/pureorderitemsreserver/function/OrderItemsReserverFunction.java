package com.chtrembl.petstore.pureorderitemsreserver.function;

import com.chtrembl.petstore.pureorderitemsreserver.model.OrderRequest;
import com.chtrembl.petstore.pureorderitemsreserver.model.OrderResponse;
import com.chtrembl.petstore.pureorderitemsreserver.service.BlobStorageService;
import com.chtrembl.petstore.pureorderitemsreserver.service.OrderItemsReserverService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.util.Optional;

/**
 * Pure Azure Function that reserves order items by uploading JSON to Azure Blob Storage.
 * No Spring Boot dependencies - uses pure Azure Functions and Azure SDK.
 */
public class OrderItemsReserverFunction {

    // Static singleton services (initialized once per function app instance)
    private static ObjectMapper objectMapper;
    private static OrderItemsReserverService orderItemsReserverService;

    /**
     * Initialize services lazily (only once per function app instance).
     */
    private synchronized void initializeServices(ExecutionContext context) {
        if (orderItemsReserverService != null) {
            return; // Already initialized
        }

        context.getLogger().info("Initializing services...");

        // Get configuration from environment variables
        String connectionString = System.getenv("AZURE_STORAGE_CONNECTION_STRING");
        String containerName = System.getenv("AZURE_STORAGE_CONTAINER_NAME");

        if (connectionString == null || connectionString.isEmpty()) {
            throw new IllegalStateException("AZURE_STORAGE_CONNECTION_STRING environment variable is not set");
        }

        if (containerName == null || containerName.isEmpty()) {
            containerName = "order-items"; // Default container name
        }

        // Initialize ObjectMapper
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Initialize services
        BlobStorageService blobStorageService = new BlobStorageService(objectMapper, connectionString, containerName);
        orderItemsReserverService = new OrderItemsReserverService(blobStorageService);

        context.getLogger().info("Services initialized successfully");
    }

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
            // Initialize services if not already done
            initializeServices(context);

            // Parse request body
            String requestBody = request.getBody().orElse(null);
            if (requestBody == null || requestBody.isEmpty()) {
                context.getLogger().warning("Empty request body received");
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body(objectMapper.writeValueAsString(
                                new OrderResponse(null, "ERROR", "Request body is required")))
                        .header("Content-Type", "application/json")
                        .build();
            }

            // Deserialize to OrderRequest
            OrderRequest orderRequest = objectMapper.readValue(requestBody, OrderRequest.class);

            // Validate required fields
            if (orderRequest.getSessionId() == null || orderRequest.getSessionId().isEmpty()) {
                context.getLogger().warning("Session ID is missing");
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body(objectMapper.writeValueAsString(
                                new OrderResponse(null, "ERROR", "Session ID is required")))
                        .header("Content-Type", "application/json")
                        .build();
            }

            if (orderRequest.getProducts() == null || orderRequest.getProducts().isEmpty()) {
                context.getLogger().warning("Product list is empty");
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body(objectMapper.writeValueAsString(
                                new OrderResponse(orderRequest.getSessionId(), "ERROR", "Product list cannot be empty")))
                        .header("Content-Type", "application/json")
                        .build();
            }

            context.getLogger().info("Processing order reservation for session: " + orderRequest.getSessionId());

            // Process the order
            OrderResponse response = orderItemsReserverService.reserveOrderItems(orderRequest);

            context.getLogger().info("Order reservation completed for session: " + orderRequest.getSessionId());

            // Determine HTTP status based on response status
            HttpStatus httpStatus = "SUCCESS".equals(response.getStatus()) ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;

            return request.createResponseBuilder(httpStatus)
                    .body(objectMapper.writeValueAsString(response))
                    .header("Content-Type", "application/json")
                    .build();

        } catch (Exception e) {
            context.getLogger().severe("Error processing order reservation: " + e.getMessage());
            e.printStackTrace();

            try {
                OrderResponse errorResponse = new OrderResponse(
                        null,
                        "ERROR",
                        "Failed to reserve order items: " + e.getMessage()
                );

                return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(objectMapper.writeValueAsString(errorResponse))
                        .header("Content-Type", "application/json")
                        .build();
            } catch (Exception jsonException) {
                // If even JSON serialization fails, return plain text
                return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("{\"status\":\"ERROR\",\"message\":\"Internal server error\"}")
                        .header("Content-Type", "application/json")
                        .build();
            }
        }
    }
}
