package com.chtrembl.petstore.order.service;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.chtrembl.petstore.order.model.OrderItemsRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for asynchronous OrderItemsReserver communication over Azure Service Bus.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderItemsReserverService {

    private final ObjectMapper objectMapper;

    @Value("${petstore.service.orderitemsreserver.connection-string}")
    private String serviceBusConnectionString;

    @Value("${petstore.service.orderitemsreserver.queue-name}")
    private String queueName;

    private ServiceBusSenderClient senderClient;

    @PostConstruct
    void initSenderClient() {
        senderClient = new ServiceBusClientBuilder()
                .connectionString(serviceBusConnectionString)
                .sender()
                .queueName(queueName)
                .buildClient();

        log.info("OrderItemsReserver Service Bus sender initialized for queue: {}", queueName);
    }

    @PreDestroy
    void closeSenderClient() {
        if (senderClient != null) {
            senderClient.close();
        }
    }

    /**
     * Reserve order items by enqueueing an async message for OrderItemsReserver.
     *
     * @param request Order items reservation request with sessionId, username, and products
     */
    public void reserveOrderItems(OrderItemsRequest request) {
        try {
            String payload = objectMapper.writeValueAsString(request);
            ServiceBusMessage message = new ServiceBusMessage(payload)
                    .setContentType("application/json")
                    .setMessageId(request.getSessionId());

            senderClient.sendMessage(message);
            log.info("Order reservation message queued for session: {}", request.getSessionId());
        } catch (Exception ex) {
            throw new RuntimeException("Failed to enqueue order reservation message", ex);
        }
    }
}
