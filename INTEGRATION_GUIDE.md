# PetStore ↔ OrderItemsReserver Integration Guide

This document explains how the PetStore application communicates with the OrderItemsReserver Azure Function to reserve order items in Azure Blob Storage.

## Architecture Overview

```
┌─────────────────┐         ┌──────────────────────┐         ┌──────────────────────┐         ┌─────────────────────┐
│                 │  HTTP   │                      │  HTTP   │  OrderItemsReserver  │  JSON   │   Azure Blob        │
│   PetStoreApp   │ ───────▶│   OrderService       │ ───────▶│   Azure Function     │ ───────▶│   Storage           │
│  (Frontend)     │  POST   │  (Microservice)      │  POST   │   (Serverless)       │  Upload │  (order-items)      │
└─────────────────┘         └──────────────────────┘         └──────────────────────┘         └─────────────────────┘
      │                               │
      │ 1. User clicks               │ 3. If order.complete=true
      │    "Complete Order"           │    Call Azure Function
      │                               │
      └─▶ ShoppingCartController      └─▶ OrderController.placeOrder()
              │                               │
              │                               ├─▶ orderService.updateOrder()
              └─▶ 2. POST to OrderService    ├─▶ enrichOrderWithProductDetails()
                                              └─▶ orderItemsReserverClient.reserveOrderItems()
```

## Integration Components

### 1. Feign Client
**File**: `petstoreorderservice/src/main/java/com/chtrembl/petstore/order/client/OrderItemsReserverClient.java`

```java
@FeignClient(
    name = "orderitemsreserver-service",
    url = "${petstore.service.orderitemsreserver.url}",
    configuration = FeignConfig.class
)
public interface OrderItemsReserverClient {
    @PostMapping("/api/orderitems/reserve")
    OrderItemsResponse reserveOrderItems(@RequestBody OrderItemsRequest request);
}
```

### 2. DTOs (Data Transfer Objects)

#### OrderItemsRequest
**File**: `petstoreorderservice/src/main/java/com/chtrembl/petstore/order/model/OrderItemsRequest.java`

Contains:
- `sessionId` - Order ID (session identifier)
- `username` - Extracted from email
- `userId` - User email from order
- `products` - List of ordered products with id, name, quantity, price

Helper method: `OrderItemsRequest.fromOrder(Order)` - Converts Order to request DTO

#### OrderItemsResponse
**File**: `petstoreorderservice/src/main/java/com/chtrembl/petstore/order/model/OrderItemsResponse.java`

Contains:
- `sessionId` - Echo of session ID
- `status` - "SUCCESS" or "ERROR"
- `message` - Success/error message

Helper method: `isSuccess()` - Returns true if status is "SUCCESS"

### 3. OrderService Integration
**File**: `petstoreorderservice/src/main/java/com/chtrembl/petstore/order/controller/OrderController.java`

**Updated Method**: `placeOrder()`

**Flow**:
1. PetStoreApp calls OrderService: `POST /petstoreorderservice/v2/store/order`
2. OrderService updates order via `orderService.updateOrder()`
3. OrderService enriches order with product details
4. **If order.complete = true AND products exist**:
   - Convert Order to OrderItemsRequest
   - Call Azure Function via Feign client
   - Log success/failure (doesn't block order processing)
5. Return enriched order to PetStoreApp

**Important**: The Azure Function call is **non-blocking**. If it fails, the order still processes to ensure good user experience.

## Configuration

### Application Configuration
**File**: `petstoreorderservice/src/main/resources/application.yml`

```yaml
petstore:
  service:
    orderitemsreserver:
      url: ${ORDERITEMSRESERVER_URL:http://localhost:7071}
```

### Environment Variables
**File**: `petstore/.env` (create from `.env.sample`)

```bash
# For local Azure Functions Core Tools
ORDERITEMSRESERVER_URL=http://localhost:7071

# For Docker Compose
ORDERITEMSRESERVER_URL=http://orderitemsreserver:8084

# For Azure deployed function
ORDERITEMSRESERVER_URL=https://petstore-orderitems-prod.azurewebsites.net
```

## Testing the Integration

### Prerequisites

1. **Azure Storage** - Running locally (Azurite) or Azure Storage Account
2. **OrderItemsReserver** - Running locally or deployed to Azure
3. **PetStoreApp** - Running locally or in Docker

### Scenario 1: All Local (Recommended for Development)

#### Step 1: Start Azurite
```bash
# Install Azurite
npm install -g azurite

# Start blob storage emulator
azurite-blob --location ./azurite-data
```

#### Step 2: Start OrderItemsReserver Function
```bash
cd petstore/orderitemsreserver

# Ensure local.settings.json is configured
# AZURE_STORAGE_CONNECTION_STRING should point to Azurite

# Start the function
mvn azure-functions:run
```

Output should show:
```
Functions:
  reserveOrderItems: [POST] http://localhost:7071/api/orderitems/reserve
```

#### Step 3: Configure PetStoreApp
```bash
cd petstore

# Create .env if not exists
cp .env.sample .env

# Edit .env
nano .env
```

Ensure:
```bash
ORDERITEMSRESERVER_URL=http://localhost:7071
```

#### Step 4: Start PetStoreApp
```bash
cd petstoreapp
mvn spring-boot:run
```

Or with Docker Compose:
```bash
cd petstore
docker-compose up petstoreapp
```

#### Step 5: Test the Flow

1. Open browser: `http://localhost:8080`
2. Browse products and add items to cart
3. Go to cart: `http://localhost:8080/cart`
4. Click **"Complete Order"** button

#### Step 6: Verify Results

**Check PetStoreApp Logs**:
```
INFO  - Calling OrderItemsReserver Azure Function for session: abc-123-def
INFO  - Order items reserved successfully in Azure Blob Storage. SessionId: abc-123-def
INFO  - Order completed successfully for user: john.doe
```

**Check Azure Function Logs**:
```
[INFO] Processing order reservation for session: abc-123-def
[INFO] Order items reserved successfully for session: abc-123-def
```

**Verify Blob Storage**:
```bash
# Using Azure Storage Explorer
# Connect to Azurite (http://127.0.0.1:10000)
# Navigate to: devstoreaccount1 → Blob Containers → order-items
# You should see: abc-123-def.json

# Or using Azure CLI
az storage blob list \
  --connection-string "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;..." \
  --container-name order-items \
  --output table
```

**Check Blob Contents**:
```bash
# Download and view the JSON
az storage blob download \
  --connection-string "..." \
  --container-name order-items \
  --name abc-123-def.json \
  --file downloaded.json

cat downloaded.json
```

Expected JSON:
```json
{
  "sessionId": "abc-123-def",
  "username": "john.doe",
  "userId": "john.doe@example.com",
  "products": [
    {
      "id": "1",
      "name": "Dog Food Premium",
      "quantity": 2,
      "price": null
    }
  ],
  "timestamp": "2026-03-28T15:30:00",
  "status": "RESERVED"
}
```

---

### Scenario 2: PetStoreApp Local + Azure Function Deployed

#### Configuration
```bash
# In petstore/.env
ORDERITEMSRESERVER_URL=https://petstore-orderitems-prod.azurewebsites.net
```

**Note**: When calling deployed Azure Functions, you may need to append function key:
```bash
ORDERITEMSRESERVER_URL=https://petstore-orderitems-prod.azurewebsites.net
```

Or configure the function to allow anonymous access (not recommended for production).

---

### Scenario 3: Full Docker Compose

If running OrderItemsReserver in Docker (Spring Boot mode for integration):

```bash
cd petstore

# Ensure .env has
ORDERITEMSRESERVER_URL=http://orderitemsreserver:8084

# Start all services
docker-compose up -d
```

---

## Troubleshooting

### Issue: "Connection refused" when calling Azure Function

**Symptoms**:
```
ERROR - Failed to call OrderItemsReserver Azure Function for session abc-123-def
feign.RetryableException: Connection refused
```

**Solutions**:
1. Verify Azure Function is running: `curl http://localhost:7071/api/orderitems/reserve`
2. Check `ORDERITEMSRESERVER_URL` in `.env` or environment
3. Check firewall/network settings

---

### Issue: Azure Function returns 500 Internal Server Error

**Symptoms**:
```
WARN - Order items reservation returned non-success status: ERROR - Failed to reserve order items
```

**Solutions**:
1. Check Azure Function logs: `mvn azure-functions:run` output
2. Verify Azure Storage connection string in `local.settings.json`
3. Ensure Azurite is running if using local emulator
4. Check blob container exists (should auto-create)

---

### Issue: Order completes but no blob created

**Symptoms**:
- Order shows as complete in PetStoreApp
- No JSON file in blob storage
- No errors in logs

**Solutions**:
1. Check if Feign client is calling the function (look for "Calling OrderItemsReserver" log)
2. Verify Feign client bean is created (Spring should auto-configure)
3. Check if `@EnableFeignClients` is present in main application class
4. Verify URL configuration in `application.yml`

---

### Issue: Empty products list in blob

**Symptoms**:
```json
{
  "products": []
}
```

**Solutions**:
- Order must have products before calling `completeCart`
- Check that products were added to cart before checkout
- Verify Order object is correctly retrieved in controller

---

## Logging and Monitoring

### PetStoreApp Logs
Key log messages to watch:
```
INFO  - Calling OrderItemsReserver Azure Function for session: {sessionId}
INFO  - Order items reserved successfully in Azure Blob Storage. SessionId: {sessionId}
WARN  - Order items reservation returned non-success status: {status}
ERROR - Failed to call OrderItemsReserver Azure Function for session {sessionId}
```

### Azure Function Logs
```
[INFO] Processing order reservation for session: {sessionId}
[INFO] Successfully uploaded order data for session: {sessionId} to blob: {fileName}
[ERROR] Failed to reserve order items for session: {sessionId}
```

### Application Insights (If Configured)
When deployed to Azure, both applications can log to Application Insights:
- Custom events: "OrderItemsReserved"
- Custom metrics: "OrderReservationDuration"
- Dependencies: HTTP calls from PetStoreApp to Azure Function

---

## API Contract

### Request
```json
POST /api/orderitems/reserve
Content-Type: application/json

{
  "sessionId": "abc-123-def",
  "username": "john.doe",
  "userId": "john.doe@example.com",
  "products": [
    {
      "id": "1",
      "name": "Dog Food Premium",
      "quantity": 2,
      "price": 29.99
    }
  ]
}
```

### Response (Success)
```json
HTTP/1.1 200 OK
Content-Type: application/json

{
  "sessionId": "abc-123-def",
  "status": "SUCCESS",
  "message": "Order items reserved successfully"
}
```

### Response (Error)
```json
HTTP/1.1 500 Internal Server Error
Content-Type: application/json

{
  "sessionId": "abc-123-def",
  "status": "ERROR",
  "message": "Failed to reserve order items: Connection timeout"
}
```

---

## Security Considerations

### Local Development
- No authentication required for local Azure Functions
- Connection to Azurite is unencrypted (localhost only)

### Azure Production
- Azure Function uses function-level authorization (requires key in URL or header)
- Storage connection uses HTTPS with Azure Storage Account keys
- Consider using Managed Identity for Function → Storage authentication
- For PetStoreApp → Function:
  - Option 1: Include function key in URL (configured in .env)
  - Option 2: Use API Management gateway with subscription keys
  - Option 3: Enable anonymous access (only for internal services behind firewall)

---

## Future Enhancements

1. **Retry Logic**: Add resilient retry with exponential backoff using Resilience4j
2. **Circuit Breaker**: Prevent cascading failures if Azure Function is down
3. **Async Processing**: Make the call non-blocking using CompletableFuture
4. **Event-Driven**: Use Azure Service Bus queue instead of direct HTTP call
5. **Price Data**: Add price information to Product model for accurate reservation
6. **Telemetry**: Track custom events in Application Insights for reservation success/failure rates

---

## Summary

✅ **Integration is complete**:
- PetStoreApp calls OrderItemsReserver Azure Function when user completes order
- Order data is uploaded to Azure Blob Storage as JSON
- Non-blocking call ensures order completion even if Azure Function fails
- Fully configurable via environment variables for different environments

🚀 **Ready to test**: Follow Scenario 1 above for local testing!
