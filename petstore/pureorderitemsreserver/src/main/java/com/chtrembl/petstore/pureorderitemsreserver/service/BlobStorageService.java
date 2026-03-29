package com.chtrembl.petstore.pureorderitemsreserver.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Service for Azure Blob Storage operations using pure Azure SDK.
 * No Spring dependencies - uses direct Azure Storage SDK.
 */
@Slf4j
public class BlobStorageService {

    private final ObjectMapper objectMapper;
    private final BlobContainerClient containerClient;
    private final String containerName;

    public BlobStorageService(ObjectMapper objectMapper, String connectionString, String containerName) {
        this.objectMapper = objectMapper;
        this.containerName = containerName;

        // Create BlobServiceClient using connection string
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();

        // Get container client
        this.containerClient = blobServiceClient.getBlobContainerClient(containerName);

        // Create container if it doesn't exist
        initializeContainer();
    }

    private void initializeContainer() {
        try {
            if (!containerClient.exists()) {
                containerClient.create();
                log.info("Created blob container: {}", containerName);
            } else {
                log.info("Using existing blob container: {}", containerName);
            }
        } catch (Exception e) {
            log.error("Failed to initialize blob container: {}", e.getMessage(), e);
            throw new IllegalStateException("Unable to initialize blob storage", e);
        }
    }

    public void uploadOrderData(String sessionId, Object orderData) throws Exception {
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
        String fileName = sessionId + ".json";
        BlobClient blobClient = containerClient.getBlobClient(fileName);

        if (!blobClient.exists()) {
            log.warn("Order data not found for session: {}", sessionId);
            return null;
        }

        return blobClient.downloadContent().toString();
    }

    public boolean deleteOrderData(String sessionId) {
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
