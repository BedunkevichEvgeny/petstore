# PetStore <-> OrderItemsReserver Integration Guide

This document explains the current asynchronous integration between `petstoreorderservice` and `orderitemsreserver` using Azure Service Bus.

## Architecture Overview

```
┌─────────────────┐         ┌──────────────────────┐        enqueue        ┌──────────────────────┐
│   PetStoreApp   │  HTTP   │   OrderService       │ ────────────────────▶ │  Azure Service Bus   │
│  (Frontend)     │ ───────▶│  (petstoreorderservice)│      JSON message   │      Queue           │
└─────────────────┘         └──────────────────────┘                       └──────────┬───────────┘
                                                                                       │ trigger
                                                                                       ▼
                                                                            ┌──────────────────────┐
                                                                            │ OrderItemsReserver   │
                                                                            │ Azure Function       │
                                                                            │ (@ServiceBusQueue...)│
                                                                            └──────────┬───────────┘
                                                                                       │ upload JSON
                                                                                       ▼
                                                                            ┌──────────────────────┐
                                                                            │ Azure Blob Storage   │
                                                                            │ container: order-items│
                                                                            └──────────────────────┘
```

## Integration Components

### 1. Message Publisher (Order Service)

**File**: `petstore/petstoreorderservice/src/main/java/com/chtrembl/petstore/order/service/OrderItemsReserverService.java`

- Uses native Azure SDK (`ServiceBusClientBuilder`, `ServiceBusSenderClient`)
- Serializes `OrderItemsRequest` as JSON
- Sends to queue `ORDERITEMSRESERVER_QUEUE_NAME`
- Fire-and-forget behavior (controller continues even if enqueue fails)

### 2. Queue Trigger Consumer (Azure Function)

**File**: `petstore/orderitemsreserver/src/main/java/com/chtrembl/petstore/orderitemsreserver/function/OrderItemsReserverHandler.java`

- Trigger annotation: `@ServiceBusQueueTrigger`
- Reads message body as JSON string
- Validates required fields (`sessionId`, `products`)
- Calls `OrderItemsReserverService.reserveOrderItems(...)` to persist blob

### 3. Message Contract

**Producer model**: `petstore/petstoreorderservice/src/main/java/com/chtrembl/petstore/order/model/OrderItemsRequest.java`

**Consumer model**: `petstore/orderitemsreserver/src/main/java/com/chtrembl/petstore/orderitemsreserver/model/OrderRequest.java`

Expected JSON payload:

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
      "price": 29.99
    }
  ]
}
```

## Configuration

### Order Service (`petstoreorderservice`)

**File**: `petstore/petstoreorderservice/src/main/resources/application.yml`

```yaml
petstore:
  service:
    orderitemsreserver:
      queue-name: ${ORDERITEMSRESERVER_QUEUE_NAME:orderitemsreserver}
      connection-string: ${ORDERITEMSRESERVER_SERVICEBUS_CONNECTION:}
```

### Function App (`orderitemsreserver`)

**File**: `petstore/orderitemsreserver/local.settings.json.sample`

Required values:

- `ORDERITEMSRESERVER_SERVICEBUS_CONNECTION` (Service Bus connection string for trigger binding)
- `ORDERITEMSRESERVER_QUEUE_NAME` (queue name)
- `AZURE_STORAGE_CONNECTION_STRING` (blob storage)
- `AZURE_STORAGE_CONTAINER_NAME` (defaults to `order-items`)

### Shared Environment Variables

**File**: `petstore/.env.sample`

```dotenv
ORDERITEMSRESERVER_QUEUE_NAME=orderitemsreserver
ORDERITEMSRESERVER_SERVICEBUS_CONNECTION=Endpoint=sb://YOUR_NAMESPACE.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=YOUR_KEY
```

## Runtime Flow

1. User completes cart in UI.
2. `petstoreapp` calls `POST /petstoreorderservice/v2/store/order`.
3. `OrderController.placeOrder()` updates and enriches order.
4. `OrderItemsReserverService.reserveOrderItems(...)` publishes queue message.
5. Azure Function `reserveOrderItems` is triggered by queue message.
6. Function writes `{sessionId}.json` blob in `order-items` container.

`OrderController` intentionally does not fail order creation when queue publish fails.

## Local Testing (Queue-Based)

### 1. Start storage emulator (or use Azure Storage)

```powershell
azurite-blob --location ./azurite-data
```

### 2. Start `orderitemsreserver` function host

```powershell
Set-Location "C:\Users\Yauhen_Bedunkevich\Azure\azure_module_2\petstore\orderitemsreserver"
mvn azure-functions:run
```

### 3. Start order service (or full app stack)

```powershell
Set-Location "C:\Users\Yauhen_Bedunkevich\Azure\azure_module_2\petstore\petstoreorderservice"
mvn spring-boot:run
```

### 4. Place an order via existing app flow

- Use UI checkout flow from `petstoreapp`, or call Order Service API directly.
- Verify logs include queue publish and function processing entries.

### 5. Verify blob output

```powershell
az storage blob list --connection-string "<YOUR_STORAGE_CONNECTION_STRING>" --container-name order-items --output table
```

## Troubleshooting

### Queue publish fails in `petstoreorderservice`

Symptoms:

- `Failed to queue OrderItemsReserver message for order ...`

Checks:

1. Verify `ORDERITEMSRESERVER_SERVICEBUS_CONNECTION`.
2. Verify queue exists and name matches `ORDERITEMSRESERVER_QUEUE_NAME`.
3. Confirm sender identity has `Send` permission.

### Function not triggered

Checks:

1. Verify `ORDERITEMSRESERVER_SERVICEBUS_CONNECTION` is configured in function settings.
2. Verify queue name in `ORDERITEMSRESERVER_QUEUE_NAME` matches sender.
3. Check function host logs for binding errors.

### Message consumed but no blob created

Checks:

1. Verify `AZURE_STORAGE_CONNECTION_STRING` and container access.
2. Check payload contains `sessionId` and non-empty `products`.
3. Review function logs for validation or storage errors.

## Security Notes

- This integration no longer uses public HTTP function endpoints.
- Protect Service Bus connection strings via Key Vault/App Settings.
- Use least-privilege SAS policies:
  - Sender (`petstoreorderservice`): `Send`
  - Function trigger (`orderitemsreserver`): `Listen`

## Summary

- Integration is asynchronous and queue-driven.
- `petstoreorderservice` publishes reservation events.
- `orderitemsreserver` consumes messages and writes blob JSON.
- Order placement remains non-blocking even if reservation enqueue fails.
