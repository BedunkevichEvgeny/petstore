package com.chtrembl.petstore.pureorderitemsreserver.service;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.chtrembl.petstore.pureorderitemsreserver.model.OrderRequest;
import com.chtrembl.petstore.pureorderitemsreserver.model.OrderResponse;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ErrorMessageSender {

    private final String connectionString;
    private final String queueName;

    public ErrorMessageSender(String connectionString, String queueName) {
        this.connectionString = connectionString;
        this.queueName = queueName;
    }

    public void sendMessage(OrderResponse orderResponse) {

        try (
            ServiceBusSenderClient senderClient = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender()
                .queueName(queueName)
                .buildClient();
        ) {
            senderClient.sendMessage(new ServiceBusMessage(orderResponse.toString()));
        } catch (Exception e) {
            log.error("Failed to create Service Bus client: {}", e.getMessage(), e);
        }
    }
}
