# Pure Order Items Reserver - Azure Functions

A pure Azure Functions implementation for reserving order items and storing them in Azure Blob Storage. **This version does NOT use Spring Boot** - it's a native Azure Functions application using only the Azure Functions Java SDK.

## Key Differences from Spring Boot Version

| Feature | Spring Boot Version | Pure Azure Functions Version |
|---------|---------------------|------------------------------|
| **Framework** | Spring Boot 3.5.0 + Spring Cloud Functions | Pure Azure Functions Java SDK |
| **Dependency Injection** | Spring IoC Container (@Autowired, @Component) | Manual initialization (static singletons) |
| **Configuration** | application.yml + Spring properties | Environment variables only |
| **Blob Storage** | Spring Cloud Azure Storage | Azure Storage SDK directly |
| **JSON Serialization** | Spring Boot auto-configured ObjectMapper | Manual ObjectMapper creation |
| **Logging** | Spring Boot + Logback (spring) | Logback (standard) + Azure Functions logger |
| **Deployment Size** | Larger (includes Spring Boot jars) | Smaller (minimal dependencies) |
| **Cold Start** | Slower (Spring context initialization) | Faster (no framework overhead) |

## Architecture

### Functions

1. **HelloFunction** (`GET /api/hello`)
   - Simple health check function
   - Returns "hi" message

2. **OrderItemsReserverFunction** (`POST /api/orderitems/reserve`)
   - Reserves order items by uploading JSON to Azure Blob Storage
   - Uses static singleton pattern for service initialization
   - Services initialized once per function app instance (not per request)

### Services

- **BlobStorageService**: Direct Azure Storage SDK client for blob operations
- **OrderItemsReserverService**: Business logic for order reservation

### Models

- **OrderRequest**: Input DTO (sessionId, username, userId, products)
- **OrderResponse**: Output DTO (sessionId, status, message)
- **OrderData**: Data stored in blob (includes timestamp and status)
- **Product**: Product DTO (id, name, quantity, price)

## Prerequisites

- Java 21
- Maven 3.9+
- Azure Functions Core Tools 4.x
- Azurite (for local blob storage emulation)

## Local Development

### 1. Setup Local Settings

```bash
cd petstore/pureorderitemsreserver
cp local.settings.json.sample local.settings.json
```

### 2. Start Azurite (Local Storage Emulator)

```bash
# Using Docker
docker run -p 10000:10000 -p 10001:10001 -p 10002:10002 mcr.microsoft.com/azure-storage/azurite

# OR using npm (if installed globally)
azurite --silent --location /tmp/azurite --debug /tmp/azurite/debug.log
```

### 3. Build the Project

```bash
mvn clean package
```

### 4. Run Locally

```bash
mvn azure-functions:run
```

The functions will be available at:
- `http://localhost:7071/api/hello`
- `http://localhost:7071/api/orderitems/reserve`

### 5. Test the Function

```bash
# Hello function
curl http://localhost:7071/api/hello

# Reserve order items
curl -X POST http://localhost:7071/api/orderitems/reserve \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "test-session-123",
    "username": "testuser",
    "userId": "user-456",
    "products": [
      {
        "id": "1",
        "name": "Dog Food",
        "quantity": 2,
        "price": 29.99
      }
    ]
  }'
```

## Deployment to Azure

### Option 1: Custom Container via ACR (Production)

**Recommended for production deployments - provides full control and custom container support**

Build and deploy using Azure Container Registry:

```bash
cd C:\Users\Yauhen_Bedunkevich\Azure\azure_module_2\petstore

# Build container in ACR and push (no local Docker needed!)
az acr build \
  --image pureorderitemsreserver:latest \
  --registry yauhenbedunkevichregistry \
  --file pureorderitemsreserver/Dockerfile \
  ./pureorderitemsreserver
```

**Requirements:**
- ✅ Azure Premium Plan (EP1) or Dedicated Plan required
- ✅ Azure Container Registry
- ✅ Cost: ~$150-200/month
- ✅ Always-on, VNet integration, custom dependencies

**📖 Complete Setup Guide:** See [CONTAINER_DEPLOYMENT.md](./CONTAINER_DEPLOYMENT.md) for full deployment instructions.

### Option 2: Maven Plugin (Consumption Plan)

```bash
# Deploy with default settings from pom.xml
mvn azure-functions:deploy

# Override function app name
mvn azure-functions:deploy -DfunctionAppName=my-pure-function-app

# Override all settings
mvn azure-functions:deploy \
  -DfunctionAppName=my-pure-function-app \
  -DfunctionResourceGroup=my-rg \
  -DfunctionAppRegion=eastus
```

**Requirements:**
- ✅ Consumption Plan supported
- ✅ No Docker needed
- ✅ Cost: ~$0.20/million executions

### Option 3: Azure CLI

```bash
# Create resource group
az group create --name my-rg --location eastus

# Create storage account
az storage account create \
  --name mypurestorage \
  --resource-group my-rg \
  --location eastus \
  --sku Standard_LRS

# Create function app
az functionapp create \
  --name my-pure-function-app \
  --resource-group my-rg \
  --consumption-plan-location eastus \
  --runtime java \
  --runtime-version 21 \
  --functions-version 4 \
  --storage-account mypurestorage

# Configure app settings
az functionapp config appsettings set \
  --name my-pure-function-app \
  --resource-group my-rg \
  --settings \
    AZURE_STORAGE_CONNECTION_STRING="<your-connection-string>" \
    AZURE_STORAGE_CONTAINER_NAME="order-items"

# Deploy
cd petstore/pureorderitemsreserver
mvn clean package azure-functions:deploy
```

## Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `AZURE_STORAGE_CONNECTION_STRING` | Yes | - | Azure Storage connection string |
| `AZURE_STORAGE_CONTAINER_NAME` | No | `order-items` | Blob container name |
| `FUNCTIONS_WORKER_RUNTIME` | Yes | `java` | Runtime (set by Azure) |
| `AzureWebJobsStorage` | Yes | - | Azure Functions internal storage |

## Blob Storage Structure

Files are stored in the configured container with the following naming:

```
<container-name>/
  └── <sessionId>.json
```

Each JSON file contains:
```json
{
  "sessionId": "session-123",
  "username": "john.doe",
  "userId": "user-456",
  "products": [
    {
      "id": "1",
      "name": "Dog Food",
      "quantity": 2,
      "price": 29.99
    }
  ],
  "timestamp": "2026-03-29T10:30:00",
  "status": "RESERVED"
}
```

## Performance Considerations

### Cold Start
Pure Azure Functions have **faster cold starts** compared to Spring Boot version because:
- No Spring context initialization
- Minimal dependencies
- Smaller deployment package

### Service Initialization
Services are initialized using **static singleton pattern**:
- Initialized once per function app instance
- Shared across all function invocations
- Thread-safe initialization with `synchronized`
- Significantly faster than Spring Boot's per-request injection

### Memory Usage
Lower memory footprint:
- No Spring framework overhead
- Direct SDK usage
- Minimal dependency tree

## Troubleshooting

### Function fails with "AZURE_STORAGE_CONNECTION_STRING environment variable is not set"
- Ensure `local.settings.json` exists (copy from `.sample`)
- For Azure deployment, set app settings via Azure Portal or CLI

### Blob upload fails
- Verify Azurite is running (local)
- Verify connection string is correct
- Check blob container name
- Review function logs for detailed errors

### Service initialization errors
- Check Azure Functions Core Tools version (should be 4.x)
- Verify Java version (should be 21)
- Check for conflicting dependencies

## Comparison: When to Use Which Version?

### Use Pure Azure Functions (this project) when:
- ✅ You want **minimal cold start time**
- ✅ You need **smallest deployment size**
- ✅ You prefer **direct SDK control**
- ✅ You're building **simple, focused functions**
- ✅ You want **lower memory usage**

### Use Spring Boot version when:
- ✅ You need **complex dependency injection**
- ✅ You want **Spring ecosystem integration** (Spring Data, Spring Security, etc.)
- ✅ You need **Spring Boot auto-configuration**
- ✅ You're familiar with **Spring conventions**
- ✅ You have **many interconnected services**

## API Documentation

### POST /api/orderitems/reserve

**Request:**
```json
{
  "sessionId": "session-123",
  "username": "john.doe",
  "userId": "user-456",
  "products": [
    {
      "id": "1",
      "name": "Dog Food",
      "quantity": 2,
      "price": 29.99
    }
  ]
}
```

**Success Response (200 OK):**
```json
{
  "sessionId": "session-123",
  "status": "SUCCESS",
  "message": "Order items reserved successfully"
}
```

**Error Response (400/500):**
```json
{
  "sessionId": "session-123",
  "status": "ERROR",
  "message": "Error details..."
}
```

## Project Structure

```
pureorderitemsreserver/
├── pom.xml                                    # Maven config (no Spring Boot parent)
├── host.json                                  # Azure Functions host config
├── local.settings.json.sample                 # Local environment variables
├── src/main/
│   ├── java/com/chtrembl/petstore/pureorderitemsreserver/
│   │   ├── function/
│   │   │   ├── HelloFunction.java             # Simple hello function
│   │   │   └── OrderItemsReserverFunction.java # Main order reservation function
│   │   ├── service/
│   │   │   ├── BlobStorageService.java        # Direct Azure SDK client
│   │   │   └── OrderItemsReserverService.java # Business logic
│   │   └── model/
│   │       ├── OrderRequest.java              # Input DTO
│   │       ├── OrderResponse.java             # Output DTO
│   │       ├── OrderData.java                 # Blob storage model
│   │       └── Product.java                   # Product DTO
│   └── resources/
│       ├── version.json                       # Build metadata
│       └── logback.xml                        # Logging configuration
└── README.md                                  # This file
```

## License

This is part of the Azure Pet Store educational project.
