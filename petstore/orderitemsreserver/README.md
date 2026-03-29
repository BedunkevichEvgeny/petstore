# Order Items Reserver - Azure Function

This is a native **Azure Function** (Java 21) with an HTTP-triggered endpoint for reserving order items and storing them in Azure Blob Storage. The function uses Spring's dependency injection for clean architecture but runs as a serverless Azure Function.

## Overview

The Order Items Reserver Azure Function:
- **HTTP-triggered function** that receives order reservation requests
- Creates a JSON document with order data including timestamp and status
- Uploads the JSON to Azure Blob Storage (overwrites if exists)
- Uses the session ID as the blob file name (e.g., `session-123.json`)
- Returns the session ID in the response
- Uses Spring DI (@Component, @Service) with Lombok for clean code
- Deploys to Azure Functions or runs locally with Azure Functions Core Tools

## Architecture

### Technology Stack
- **Runtime**: Java 21
- **Framework**: Azure Functions + Spring Boot (core DI only, no web server)
- **Dependencies**:
  - Azure Functions Java Library
  - Azure Blob Storage SDK
  - Spring Boot Starter (DI and configuration)
  - Jackson (JSON serialization with JSR310 support)
  - Lombok (code generation)

### Project Structure
```
src/main/java/com/chtrembl/petstore/orderitemsreserver/
├── function/
│   └── OrderItemsReserverFunction.java    # Azure Function HTTP trigger
├── service/
│   ├── OrderItemsReserverService.java     # Business logic layer
│   └── BlobStorageService.java            # Azure Blob Storage operations
├── model/
│   ├── OrderRequest.java                  # Request DTO
│   ├── OrderResponse.java                 # Response DTO
│   ├── OrderData.java                     # Internal model for storage
│   └── Product.java                       # Product model
└── config/
    └── ObjectMapperConfig.java            # Jackson configuration bean
```

## Azure Function Endpoint

### Reserve Order Items (HTTP Trigger)

**Function Name**: `reserveOrderItems`
**HTTP Method**: POST
**Route**: `/api/orderitems/reserve`
**Authorization Level**: Function (requires function key in Azure)

#### Request Body
```json
{
  "sessionId": "abc123",
  "username": "john.doe",
  "userId": "user-123",
  "products": [
    {
      "id": "prod-1",
      "name": "Dog Food",
      "quantity": 2,
      "price": 29.99
    },
    {
      "id": "prod-2",
      "name": "Cat Toy",
      "quantity": 1,
      "price": 9.99
    }
  ]
}
```

#### Response
```json
{
  "sessionId": "abc123",
  "status": "SUCCESS",
  "message": "Order items reserved successfully"
}
```

#### Validation Rules
- `sessionId`: Required, cannot be blank
- `products`: Required, cannot be empty array

## Configuration

### Local Development (local.settings.json)

Create or update `local.settings.json` (not committed to git):

```json
{
  "IsEncrypted": false,
  "Values": {
    "AzureWebJobsStorage": "",
    "FUNCTIONS_WORKER_RUNTIME": "java",
    "FUNCTIONS_EXTENSION_VERSION": "~4",
    "AZURE_STORAGE_CONNECTION_STRING": "YOUR_CONNECTION_STRING_HERE",
    "AZURE_STORAGE_CONTAINER_NAME": "order-items"
  }
}
```

Use `local.settings.json.sample` as a template.

### Azure Deployment (Application Settings)

When deployed to Azure, configure these Application Settings:

| Setting | Description | Required |
|---------|-------------|----------|
| `AZURE_STORAGE_CONNECTION_STRING` | Azure Storage connection string | Yes |
| `AZURE_STORAGE_CONTAINER_NAME` | Blob container name (default: order-items) | No |
| `FUNCTIONS_EXTENSION_VERSION` | Functions runtime version (~4) | Yes |
| `FUNCTIONS_WORKER_RUNTIME` | Worker runtime (java) | Yes |

### Azure Storage Setup

#### Option 1: Azure Storage Account

1. **Create a Storage Account** in Azure Portal
2. **Get the connection string**:
   - Navigate to: Storage Account → Security + networking → Access keys
   - Copy "Connection string" from key1 or key2
3. **Add to local.settings.json** or Azure Application Settings

#### Option 2: Local Development with Azurite

```bash
# Install Azurite emulator
npm install -g azurite

# Start Azurite blob service
azurite-blob --location ./azurite-data

# Use Azurite connection string in local.settings.json
"AZURE_STORAGE_CONNECTION_STRING": "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1;"
```

## Building and Running

### Prerequisites

- **Java 21** (JDK)
- **Maven 3.9+**
- **Azure Functions Core Tools v4** - [Install](https://docs.microsoft.com/en-us/azure/azure-functions/functions-run-local)
- **Azure Storage Account** or **Azurite** emulator

### Local Development

```bash
# 1. Build the function
mvn clean package

# 2. Run locally with Azure Functions Core Tools
mvn azure-functions:run

# Alternative: Navigate to the output directory and run
cd target/azure-functions/orderitemsreserver-*/
func start
```

The function will start on `http://localhost:7071`

Logs will show:
```
Functions:
  reserveOrderItems: [POST] http://localhost:7071/api/orderitems/reserve
```

### Deploy to Azure

**Quick Deploy (uses defaults from pom.xml):**
```bash
# 1. Login to Azure CLI (first time only)
az login

# 2. Build and deploy
mvn clean package azure-functions:deploy
```

This deploys to:
- **Function App**: `petstore-orderitemsreserver`
- **Resource Group**: `petstore-rg`
- **Region**: `eastus`

**Deploy to Custom Environment:**
```bash
# Override with your own values
mvn clean package azure-functions:deploy \
  -DfunctionAppName=your-unique-name \
  -DfunctionResourceGroup=your-rg \
  -DfunctionAppRegion=eastus
```

**📚 For detailed deployment options (multiple environments, CI/CD, Azure Portal setup), see [DEPLOYMENT.md](DEPLOYMENT.md)**

### Debug Locally in IDE

1. Run `mvn clean package` to generate the function metadata
2. Set breakpoints in your IDE
3. Start Azure Functions host in debug mode:
   ```bash
   mvn azure-functions:run
   ```
4. Attach debugger to the Java process

## Testing the Function

### Local Testing with curl

```bash
# Test function locally (no auth required)
curl -X POST http://localhost:7071/api/orderitems/reserve \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "test-session-123",
    "username": "testuser",
    "userId": "user-456",
    "products": [
      {
        "id": "prod-1",
        "name": "Dog Food",
        "quantity": 2,
        "price": 29.99
      }
    ]
  }'
```

Expected response:
```json
{
  "sessionId": "test-session-123",
  "status": "SUCCESS",
  "message": "Order items reserved successfully"
}
```

### Azure Testing with curl

```bash
# Get function URL from Azure Portal or use:
# https://<function-app-name>.azurewebsites.net/api/orderitems/reserve

# Requires function key (get from Azure Portal → Function → Function Keys)
curl -X POST "https://your-function-app.azurewebsites.net/api/orderitems/reserve?code=YOUR_FUNCTION_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "test-session-123",
    "username": "testuser",
    "userId": "user-456",
    "products": [
      {
        "id": "prod-1",
        "name": "Dog Food",
        "quantity": 2,
        "price": 29.99
      }
    ]
  }'
```

### Verify Blob Storage

Check that the JSON file was created:

**Using Azure Storage Explorer**:
1. Connect to your storage account
2. Navigate to `order-items` container
3. Look for `test-session-123.json`

**Using Azure CLI**:
```bash
az storage blob list \
  --account-name YOUR_ACCOUNT \
  --container-name order-items \
  --output table
```

## Blob Storage Structure

The function creates JSON files in the configured container:

```
Container: order-items (default)
├── abc123.json
├── xyz789.json
└── test-session-123.json
```

Each blob contains:
```json
{
  "sessionId": "abc123",
  "username": "john.doe",
  "userId": "user-123",
  "products": [
    {
      "id": "prod-1",
      "name": "Dog Food",
      "quantity": 2,
      "price": 29.99
    }
  ],
  "timestamp": "2026-03-28T14:30:00",
  "status": "RESERVED"
}
```

**Note**: Files are overwritten if a request with the same sessionId is received.

## Monitoring and Logging

### Local Development
```bash
# View detailed logs
mvn azure-functions:run --debug

# Or with func CLI
func start --verbose
```

### Azure Production

**Application Insights** (automatic when deployed):
- Navigate to: Azure Portal → Function App → Application Insights
- View: Live Metrics, Failures, Performance, Logs

**View Logs**:
```bash
# Stream logs from Azure
func azure functionapp logstream <function-app-name>

# Or use Azure CLI
az webapp log tail --name <function-app-name> --resource-group <resource-group>
```

## Integration with Pet Store App

To call this function from the PetStoreApp frontend:

### 1. Add Environment Variable

In `petstore/.env`:
```bash
ORDERITEMSRESERVER_URL=https://your-function-app.azurewebsites.net
ORDERITEMSRESERVER_FUNCTION_KEY=your-function-key
```

### 2. Create Feign Client

```java
@FeignClient(name = "orderitemsreserver", url = "${orderitemsreserver.url}")
public interface OrderItemsReserverClient {
    @PostMapping("/api/orderitems/reserve?code=${orderitemsreserver.key}")
    OrderResponse reserveOrderItems(@RequestBody OrderRequest request);
}
```

### 3. Call from Service

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderItemsReserverClient orderItemsReserverClient;
    private final User sessionUser; // Session-scoped

    public void processOrder(List<Product> products) {
        OrderRequest request = new OrderRequest(
            sessionUser.getSessionId(),
            sessionUser.getName(),
            sessionUser.getId(),
            products
        );

        OrderResponse response = orderItemsReserverClient.reserveOrderItems(request);
        // Handle response...
    }
}
```

## Error Handling

The function returns appropriate HTTP status codes:

| Status Code | Description |
|-------------|-------------|
| `200 OK` | Order items reserved successfully |
| `400 Bad Request` | Invalid request (missing sessionId, empty products) |
| `500 Internal Server Error` | Blob storage error or unexpected exception |

Error response format:
```json
{
  "sessionId": "abc123",
  "status": "ERROR",
  "message": "Failed to reserve order items: Connection timeout"
}
```

## Troubleshooting

### Function won't start locally
- Ensure Azure Functions Core Tools v4 is installed: `func --version`
- Check Java version: `java -version` (must be 21)
- Verify `local.settings.json` exists and is valid JSON

### Blob storage connection fails
- Check `AZURE_STORAGE_CONNECTION_STRING` is set correctly
- For Azurite: ensure it's running (`azurite-blob --version`)
- Test connection with Azure Storage Explorer

### Function deploys but returns 500
- Check Application Insights logs in Azure Portal
- Verify Azure Storage connection string in Application Settings
- Ensure storage account is accessible from Azure Functions region

## Maven Commands Reference

```bash
# Clean build
mvn clean package

# Run locally
mvn azure-functions:run

# Package for deployment
mvn azure-functions:package

# Deploy to Azure
mvn azure-functions:deploy

# Run tests (when implemented)
mvn test
```

## Additional Resources

- [Azure Functions Java Developer Guide](https://docs.microsoft.com/en-us/azure/azure-functions/functions-reference-java)
- [Azure Blob Storage for Java](https://docs.microsoft.com/en-us/azure/storage/blobs/storage-quickstart-blobs-java)
- [Azure Functions Core Tools](https://github.com/Azure/azure-functions-core-tools)
