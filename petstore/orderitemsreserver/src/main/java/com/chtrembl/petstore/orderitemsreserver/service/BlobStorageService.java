package com.chtrembl.petstore.orderitemsreserver.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Service for Azure Blob Storage operations using Spring Cloud Azure.
 * BlobServiceClient is auto-configured by Spring Cloud Azure starter.
 */
@Service
@Slf4j
public class BlobStorageService {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private BlobServiceClient blobServiceClient;

    @Value("${spring.cloud.azure.storage.blob.container-name:order-items}")
    private String containerName;

    private BlobContainerClient containerClient;

    /**
     * Initialize blob container client after bean construction.
     * Creates container if it doesn't exist.
     */
    @PostConstruct
    public void init() {
        try {
            this.containerClient = blobServiceClient.getBlobContainerClient(containerName);

            // Create container if it doesn't exist
            if (!containerClient.exists()) {
                containerClient.create();
                log.info("Created blob container: {}", containerName);
            } else {
                log.info("Using existing blob container: {}", containerName);
            }
        } catch (Exception e) {
            log.error("Failed to initialize blob storage service: {}", e.getMessage(), e);
            throw new IllegalStateException("Unable to initialize blob storage", e);
        }
    }

    public void uploadOrderData(String sessionId, Object orderData) throws Exception {
        if (containerClient == null) {
            throw new IllegalStateException("Blob storage is not properly configured");
        }

        String fileName = sessionId + ".json";
        BlobClient blobClient = containerClient.getBlobClient(fileName);

        // Convert object to JSON
        String jsonData = objectMapper.writeValueAsString(orderData);
        byte[] data = jsonData.getBytes();

        // Upload blob (overwrites if exists)
        try (InputStream inputStream = new ByteArrayInputStream(data)) {
            blobClient.upload(inputStream, data.length, true);
            log.info("Successfully uploaded order data for session: {} to blob: {}", sessionId, fileName);
        }
    }

    public String downloadOrderData(String sessionId) throws Exception {
        if (containerClient == null) {
            throw new IllegalStateException("Blob storage is not properly configured");
        }

        String fileName = sessionId + ".json";
        BlobClient blobClient = containerClient.getBlobClient(fileName);

        if (!blobClient.exists()) {
            log.warn("Order data not found for session: {}", sessionId);
            return null;
        }

        return blobClient.downloadContent().toString();
    }

    public boolean deleteOrderData(String sessionId) {
        if (containerClient == null) {
            return false;
        }

        String fileName = sessionId + ".json";
        BlobClient blobClient = containerClient.getBlobClient(fileName);

        if (blobClient.exists()) {
            blobClient.delete();
            log.info("Deleted order data for session: {}", sessionId);
            return true;
        }

        return false;
    }
}
