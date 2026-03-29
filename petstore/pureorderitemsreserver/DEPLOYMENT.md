# Pure Order Items Reserver - Deployment Guide

Complete deployment guide for the Pure Azure Functions Order Items Reserver service.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Azure Resources Setup](#azure-resources-setup)
3. [Local Development Setup](#local-development-setup)
4. [Deploy to Azure](#deploy-to-azure)
5. [Configuration](#configuration)
6. [Testing](#testing)
7. [Monitoring](#monitoring)
8. [Troubleshooting](#troubleshooting)

## Prerequisites

### Required Tools

- **Java 21**: `java --version`
- **Maven 3.9+**: `mvn --version`
- **Azure CLI**: `az --version`
- **Azure Functions Core Tools 4.x**: `func --version`
- **Docker** (for local Azurite): `docker --version`

### Azure Subscription

- Active Azure subscription
- Sufficient permissions to create resources

## Azure Resources Setup

### 1. Login to Azure

```bash
az login
az account set --subscription "<your-subscription-id>"
```

### 2. Create Resource Group

```bash
az group create \
  --name petstore-rg \
  --location eastus
```

### 3. Create Storage Account for Blob Storage

```bash
# Storage account for order items blobs
az storage account create \
  --name petstorepureblobs \
  --resource-group petstore-rg \
  --location eastus \
  --sku Standard_LRS \
  --kind StorageV2

# Get connection string
az storage account show-connection-string \
  --name petstorepureblobs \
  --resource-group petstore-rg \
  --query connectionString \
  --output tsv
```

Save this connection string - you'll need it for configuration.

### 4. Create Storage Account for Azure Functions

```bash
# Storage account for Azure Functions internal use
az storage account create \
  --name petstorepurefunc \
  --resource-group petstore-rg \
  --location eastus \
  --sku Standard_LRS
```

### 5. Create Function App

```bash
az functionapp create \
  --name petstore-pure-orderitems \
  --resource-group petstore-rg \
  --consumption-plan-location eastus \
  --runtime java \
  --runtime-version 21 \
  --functions-version 4 \
  --storage-account petstorepurefunc \
  --os-type Linux
```

## Local Development Setup

### 1. Install Azurite (Local Storage Emulator)

**Option A: Docker (Recommended)**

```bash
docker run -d -p 10000:10000 -p 10001:10001 -p 10002:10002 \
  --name azurite \
  mcr.microsoft.com/azure-storage/azurite
```

**Option B: NPM**

```bash
npm install -g azurite
azurite --silent --location /tmp/azurite --debug /tmp/azurite/debug.log
```

### 2. Configure Local Settings

```bash
cd petstore/pureorderitemsreserver
cp local.settings.json.sample local.settings.json
```

Edit `local.settings.json`:

```json
{
  "IsEncrypted": false,
  "Values": {
    "AzureWebJobsStorage": "UseDevelopmentStorage=true",
    "FUNCTIONS_WORKER_RUNTIME": "java",
    "AZURE_STORAGE_CONNECTION_STRING": "UseDevelopmentStorage=true",
    "AZURE_STORAGE_CONTAINER_NAME": "order-items"
  }
}
```

### 3. Build and Run Locally

```bash
# Build
mvn clean package

# Run locally
mvn azure-functions:run
```

Functions will be available at:
- Hello: `http://localhost:7071/api/hello`
- Reserve Order: `http://localhost:7071/api/orderitems/reserve`

### 4. Test Locally

```bash
# Test hello endpoint
curl http://localhost:7071/api/hello

# Test order reservation
curl -X POST http://localhost:7071/api/orderitems/reserve \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "local-test-001",
    "username": "testuser",
    "userId": "user-123",
    "products": [
      {
        "id": "1",
        "name": "Dog Food",
        "quantity": 2,
        "price": 29.99
      },
      {
        "id": "2",
        "name": "Cat Toy",
        "quantity": 1,
        "price": 9.99
      }
    ]
  }'
```

Expected response:
```json
{
  "sessionId": "local-test-001",
  "status": "SUCCESS",
  "message": "Order items reserved successfully"
}
```

## Deploy to Azure

### Method 1: Maven Plugin (Recommended)

```bash
cd petstore/pureorderitemsreserver

# Build and deploy
mvn clean package azure-functions:deploy \
  -DfunctionAppName=petstore-pure-orderitems \
  -DfunctionResourceGroup=petstore-rg \
  -DfunctionAppRegion=eastus
```

### Method 2: Azure CLI

```bash
# Build
mvn clean package

# Deploy
az functionapp deployment source config-zip \
  --name petstore-pure-orderitems \
  --resource-group petstore-rg \
  --src target/azure-functions/pureorderitemsreserver-0.0.1-SNAPSHOT.zip
```

### Method 3: CI/CD with GitHub Actions

Create `.github/workflows/deploy-pure-function.yml`:

```yaml
name: Deploy Pure Order Items Reserver

on:
  push:
    branches:
      - main
    paths:
      - 'petstore/pureorderitemsreserver/**'

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Java 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Build with Maven
        run: |
          cd petstore/pureorderitemsreserver
          mvn clean package

      - name: Deploy to Azure Functions
        uses: Azure/functions-action@v1
        with:
          app-name: petstore-pure-orderitems
          package: petstore/pureorderitemsreserver/target/azure-functions/pureorderitemsreserver-0.0.1-SNAPSHOT
          publish-profile: ${{ secrets.AZURE_FUNCTIONAPP_PUBLISH_PROFILE }}
```

## Configuration

### Set Application Settings in Azure

```bash
# Get blob storage connection string from earlier
BLOB_CONNECTION_STRING=$(az storage account show-connection-string \
  --name petstorepureblobs \
  --resource-group petstore-rg \
  --query connectionString \
  --output tsv)

# Configure function app
az functionapp config appsettings set \
  --name petstore-pure-orderitems \
  --resource-group petstore-rg \
  --settings \
    AZURE_STORAGE_CONNECTION_STRING="$BLOB_CONNECTION_STRING" \
    AZURE_STORAGE_CONTAINER_NAME="order-items" \
    FUNCTIONS_WORKER_RUNTIME="java"
```

### Environment Variables Reference

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `AZURE_STORAGE_CONNECTION_STRING` | ✅ Yes | - | Connection string for blob storage |
| `AZURE_STORAGE_CONTAINER_NAME` | No | `order-items` | Container name for order JSON files |
| `FUNCTIONS_WORKER_RUNTIME` | ✅ Yes | `java` | Azure Functions runtime |
| `AzureWebJobsStorage` | ✅ Yes | - | Internal storage (auto-configured) |

## Testing

### Test Azure Deployment

```bash
# Get function URL
FUNCTION_URL=$(az functionapp function show \
  --name petstore-pure-orderitems \
  --resource-group petstore-rg \
  --function-name reserveOrderItems \
  --query invokeUrlTemplate \
  --output tsv)

# Test hello function
curl "https://petstore-pure-orderitems.azurewebsites.net/api/hello"

# Test order reservation
curl -X POST "https://petstore-pure-orderitems.azurewebsites.net/api/orderitems/reserve" \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "azure-test-001",
    "username": "testuser",
    "userId": "user-123",
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

### Verify Blob Storage

```bash
# List blobs in container
az storage blob list \
  --account-name petstorepureblobs \
  --container-name order-items \
  --output table

# Download a specific order file
az storage blob download \
  --account-name petstorepureblobs \
  --container-name order-items \
  --name "azure-test-001.json" \
  --file downloaded-order.json

# View contents
cat downloaded-order.json
```

## Monitoring

### View Function Logs

```bash
# Stream logs in real-time
az functionapp log tail \
  --name petstore-pure-orderitems \
  --resource-group petstore-rg
```

### Application Insights (Optional)

```bash
# Create Application Insights
az monitor app-insights component create \
  --app petstore-pure-insights \
  --location eastus \
  --resource-group petstore-rg

# Get instrumentation key
INSTRUMENTATION_KEY=$(az monitor app-insights component show \
  --app petstore-pure-insights \
  --resource-group petstore-rg \
  --query instrumentationKey \
  --output tsv)

# Configure function app
az functionapp config appsettings set \
  --name petstore-pure-orderitems \
  --resource-group petstore-rg \
  --settings \
    APPINSIGHTS_INSTRUMENTATIONKEY="$INSTRUMENTATION_KEY"
```

### View Metrics in Azure Portal

1. Navigate to Function App: `petstore-pure-orderitems`
2. Click **Metrics**
3. Add metrics:
   - Function Execution Count
   - Function Execution Units
   - Http 5xx Errors
   - Average Response Time

## Troubleshooting

### Function Fails with "Connection String Not Set"

**Problem:** `AZURE_STORAGE_CONNECTION_STRING environment variable is not set`

**Solution:**
```bash
# Verify app settings
az functionapp config appsettings list \
  --name petstore-pure-orderitems \
  --resource-group petstore-rg \
  --query "[?name=='AZURE_STORAGE_CONNECTION_STRING']"

# If missing, set it
az functionapp config appsettings set \
  --name petstore-pure-orderitems \
  --resource-group petstore-rg \
  --settings AZURE_STORAGE_CONNECTION_STRING="<connection-string>"
```

### Cold Start Performance

**Issue:** First request after idle period is slow

**Solutions:**
1. **Use Premium Plan** for always-ready instances
2. **Enable Always On** (requires App Service Plan)
3. **Optimize service initialization** (already done with static singletons)

```bash
# Upgrade to Premium plan
az functionapp plan create \
  --name petstore-premium-plan \
  --resource-group petstore-rg \
  --location eastus \
  --sku EP1

az functionapp update \
  --name petstore-pure-orderitems \
  --resource-group petstore-rg \
  --plan petstore-premium-plan
```

### Blob Upload Failures

**Check container exists:**
```bash
az storage container show \
  --name order-items \
  --account-name petstorepureblobs
```

**Create if missing:**
```bash
az storage container create \
  --name order-items \
  --account-name petstorepureblobs
```

### Maven Build Fails

**Clear Maven cache and rebuild:**
```bash
mvn clean
rm -rf ~/.m2/repository/com/chtrembl/petstore
mvn package
```

### Function App Not Responding

**Restart function app:**
```bash
az functionapp restart \
  --name petstore-pure-orderitems \
  --resource-group petstore-rg
```

## Performance Benchmarks

### Cold Start Times (approximate)

| Metric | Pure Azure Functions | Spring Boot Version |
|--------|---------------------|---------------------|
| First request (cold start) | ~2-3 seconds | ~5-8 seconds |
| Subsequent requests | ~50-100ms | ~50-100ms |
| Memory usage | ~150-200 MB | ~300-400 MB |
| Package size | ~15 MB | ~50 MB |

### Load Testing

Use Azure Load Testing or K6:

```bash
# Install k6
brew install k6  # macOS
# or download from https://k6.io

# Run load test
k6 run tests/load-test-pure-function.js
```

## Clean Up Resources

```bash
# Delete entire resource group (WARNING: deletes all resources)
az group delete --name petstore-rg --yes --no-wait

# Or delete individual resources
az functionapp delete \
  --name petstore-pure-orderitems \
  --resource-group petstore-rg

az storage account delete \
  --name petstorepureblobs \
  --resource-group petstore-rg
```

## Next Steps

1. **Integrate with OrderService**: Update `ORDERITEMSRESERVER_URL` in OrderService to point to this function
2. **Add Authentication**: Configure function-level or app-level authentication
3. **Set up CI/CD**: Implement GitHub Actions or Azure DevOps pipeline
4. **Monitor Performance**: Set up Application Insights dashboards
5. **Add Tests**: Create integration tests using Azure Functions test framework

## References

- [Azure Functions Java Developer Guide](https://learn.microsoft.com/azure/azure-functions/functions-reference-java)
- [Azure Storage Java SDK](https://learn.microsoft.com/java/api/overview/azure/storage-blob-readme)
- [Azure Functions Best Practices](https://learn.microsoft.com/azure/azure-functions/functions-best-practices)
