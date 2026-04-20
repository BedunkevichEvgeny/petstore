package com.chtrembl.petstore.pureorderitemsreserver.function;

import com.chtrembl.petstore.pureorderitemsreserver.model.OrderRequest;
import com.chtrembl.petstore.pureorderitemsreserver.model.OrderResponse;
import com.chtrembl.petstore.pureorderitemsreserver.service.BlobStorageService;
import com.chtrembl.petstore.pureorderitemsreserver.service.ErrorMessageSender;
import com.chtrembl.petstore.pureorderitemsreserver.service.OrderItemsReserverService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.ServiceBusQueueTrigger;

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
        String queueName = System.getenv("ORDERITEMSRESERVER_ERROR_QUEUE_NAME");
        String serviceBusConnectionString = System.getenv("ORDERITEMSRESERVER_SERVICEBUS_CONNECTION");

        if (connectionString == null || connectionString.isEmpty()) {
            throw new IllegalStateException("AZURE_STORAGE_CONNECTION_STRING environment variable is not set");
        }

        if (containerName == null || containerName.isEmpty()) {
            containerName = "order-items"; // Default container name
        }

        if (queueName == null || queueName.isEmpty()) {
            throw new IllegalStateException("ORDERITEMSRESERVER_ERROR_QUEUE_NAME environment variable is not set");
        }

        if (serviceBusConnectionString == null || serviceBusConnectionString.isEmpty()) {
            throw new IllegalStateException("ORDERITEMSRESERVER_SERVICEBUS_CONNECTION environment variable is not set");
        }

        // Initialize ObjectMapper
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());


        // Initialize services
        ErrorMessageSender errorMessageSender = new ErrorMessageSender(serviceBusConnectionString, queueName);
        BlobStorageService blobStorageService = new BlobStorageService(objectMapper, connectionString, containerName);
        orderItemsReserverService = new OrderItemsReserverService(blobStorageService, errorMessageSender);

        context.getLogger().info("Services initialized successfully");
    }

    @FunctionName("reserveOrderItems")
    public void execute(
        @ServiceBusQueueTrigger(
            name = "message",
            queueName = "%ORDERITEMSRESERVER_QUEUE_NAME%",
            connection = "ORDERITEMSRESERVER_SERVICEBUS_CONNECTION"
        ) String requestBody,
        final ExecutionContext context) {

        context.getLogger().info("Processing order items reservation request");

        try {
            // Initialize services if not already done
            initializeServices(context);

            // Parse request body
            if (requestBody == null || requestBody.isEmpty()) {
                context.getLogger().warning("Empty request body received");
                return;
            }

            // Deserialize to OrderRequest
            OrderRequest orderRequest = objectMapper.readValue(requestBody, OrderRequest.class);

            // Validate required fields
            if (orderRequest.getSessionId() == null || orderRequest.getSessionId().isEmpty()) {
                context.getLogger().warning("Session ID is missing");
                return;
            }

            if (orderRequest.getProducts() == null || orderRequest.getProducts().isEmpty()) {
                context.getLogger().warning("Product list is empty");
                return;
            }

            context.getLogger().info("Processing order reservation for session: " + orderRequest.getSessionId());

            // Process the order
            OrderResponse response = orderItemsReserverService.reserveOrderItems(orderRequest);

            if (response.isSuccess()) {
                context.getLogger().info("Order reservation completed for session: " + orderRequest.getSessionId());
            } else {
                context.getLogger().warning("Order reservation failed for session: " + orderRequest.getSessionId());
            }

        } catch (Exception e) {
            context.getLogger().severe("Error processing order reservation: " + e.getMessage());
        }
    }
}
