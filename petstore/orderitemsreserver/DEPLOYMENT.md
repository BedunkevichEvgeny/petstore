# Azure Function Deployment Guide

This document explains how to deploy the Order Items Reserver Azure Function to Azure.

## Deployment Options

You have **three ways** to configure and deploy the Azure Function:

### Option 1: Deploy with pom.xml Defaults ✅ Recommended for Learning

The `pom.xml` has default values configured:

```xml
<properties>
    <functionAppName>petstore-orderitemsreserver</functionAppName>
    <functionResourceGroup>petstore-rg</functionResourceGroup>
    <functionAppRegion>eastus</functionAppRegion>
</properties>
```

**Deploy directly:**
```bash
# Plugin will create Function App if it doesn't exist
mvn clean package azure-functions:deploy
```

**Pros:**
- Simple, one-command deployment
- Good for development and learning
- Maven plugin handles everything

**Cons:**
- Less control over infrastructure (networking, identity, etc.)
- Not ideal for production environments

---

### Option 2: Override via Command Line ✅ Recommended for Multiple Environments

Override the pom.xml defaults with Maven properties:

```bash
# Deploy to dev environment
mvn clean package azure-functions:deploy \
  -DfunctionAppName=petstore-orderitems-dev \
  -DfunctionResourceGroup=petstore-dev-rg \
  -DfunctionAppRegion=eastus

# Deploy to production environment
mvn clean package azure-functions:deploy \
  -DfunctionAppName=petstore-orderitems-prod \
  -DfunctionResourceGroup=petstore-prod-rg \
  -DfunctionAppRegion=westeurope
```

**Pros:**
- Flexible for multiple environments
- Same pom.xml works for dev/staging/prod
- Easy to script in CI/CD pipelines

**Cons:**
- Requires remembering/scripting the parameters

---

### Option 3: Create Infrastructure First, Then Deploy ✅ Recommended for Production

Create the Function App through Azure Portal/CLI/Terraform first, then deploy code to it.

#### Step 1: Create Function App via Azure Portal

1. Go to **Azure Portal** → **Create a resource** → **Function App**
2. Configure:
   - **Resource Group**: `petstore-rg` (or create new)
   - **Function App name**: `petstore-orderitems-prod` (must be globally unique)
   - **Runtime stack**: Java
   - **Version**: 21
   - **Region**: East US (or your preference)
   - **Operating System**: Linux
   - **Plan type**: Consumption (Serverless) or Premium
3. **Review + Create**

4. Configure **Application Settings**:
   - Go to: Function App → Configuration → Application settings
   - Add:
     - `AZURE_STORAGE_CONNECTION_STRING`: Your storage connection string
     - `AZURE_STORAGE_CONTAINER_NAME`: `order-items`
     - `FUNCTIONS_WORKER_RUNTIME`: `java`
     - `FUNCTIONS_EXTENSION_VERSION`: `~4`

#### Step 2: Deploy Code to Existing App

```bash
# Deploy to the existing Function App
mvn clean package azure-functions:deploy \
  -DfunctionAppName=petstore-orderitems-prod \
  -DfunctionResourceGroup=petstore-rg
```

#### Step 1 (Alternative): Create Function App via Azure CLI

```bash
# Variables
RESOURCE_GROUP="petstore-rg"
FUNCTION_APP_NAME="petstore-orderitems-prod"
STORAGE_ACCOUNT_NAME="petstorestorageprod"
REGION="eastus"

# Create resource group
az group create --name $RESOURCE_GROUP --location $REGION

# Create storage account
az storage account create \
  --name $STORAGE_ACCOUNT_NAME \
  --resource-group $RESOURCE_GROUP \
  --location $REGION \
  --sku Standard_LRS

# Create Function App
az functionapp create \
  --name $FUNCTION_APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --storage-account $STORAGE_ACCOUNT_NAME \
  --consumption-plan-location $REGION \
  --runtime java \
  --runtime-version 21 \
  --functions-version 4 \
  --os-type Linux

# Get storage connection string
STORAGE_CONNECTION=$(az storage account show-connection-string \
  --name $STORAGE_ACCOUNT_NAME \
  --resource-group $RESOURCE_GROUP \
  --query connectionString \
  --output tsv)

# Configure application settings
az functionapp config appsettings set \
  --name $FUNCTION_APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --settings \
    "AZURE_STORAGE_CONNECTION_STRING=$STORAGE_CONNECTION" \
    "AZURE_STORAGE_CONTAINER_NAME=order-items" \
    "FUNCTIONS_WORKER_RUNTIME=java"

# Deploy code
mvn clean package azure-functions:deploy \
  -DfunctionAppName=$FUNCTION_APP_NAME \
  -DfunctionResourceGroup=$RESOURCE_GROUP
```

**Pros:**
- Full control over infrastructure
- Can configure networking, managed identity, Key Vault integration
- Better for production environments
- Separates infrastructure provisioning from code deployment

**Cons:**
- More initial setup required
- Need to manage infrastructure separately

---

## Deployment Workflow

### First Time Deployment

```bash
# 1. Login to Azure
az login

# 2. Set your subscription (if you have multiple)
az account set --subscription "Your Subscription Name"

# 3. Deploy (creates resources if they don't exist)
mvn clean package azure-functions:deploy
```

### Update Existing Function

```bash
# Just redeploy - updates code only
mvn clean package azure-functions:deploy
```

---

## Environment-Specific Deployment

### Using Maven Profiles (Optional Enhancement)

Add to `pom.xml`:

```xml
<profiles>
    <profile>
        <id>dev</id>
        <properties>
            <functionAppName>petstore-orderitems-dev</functionAppName>
            <functionResourceGroup>petstore-dev-rg</functionResourceGroup>
            <functionAppRegion>eastus</functionAppRegion>
        </properties>
    </profile>
    <profile>
        <id>prod</id>
        <properties>
            <functionAppName>petstore-orderitems-prod</functionAppName>
            <functionResourceGroup>petstore-prod-rg</functionResourceGroup>
            <functionAppRegion>eastus</functionAppRegion>
        </properties>
    </profile>
</profiles>
```

Deploy with profile:
```bash
# Deploy to dev
mvn clean package azure-functions:deploy -Pdev

# Deploy to prod
mvn clean package azure-functions:deploy -Pprod
```

---

## CI/CD Pipeline Example (GitHub Actions)

```yaml
name: Deploy Azure Function

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Azure Login
        uses: azure/login@v1
        with:
          creds: ${{ secrets.AZURE_CREDENTIALS }}

      - name: Deploy to Azure Functions
        run: |
          cd petstore/orderitemsreserver
          mvn clean package azure-functions:deploy \
            -DfunctionAppName=${{ secrets.FUNCTION_APP_NAME }} \
            -DfunctionResourceGroup=${{ secrets.RESOURCE_GROUP }}
```

---

## Verify Deployment

After deployment:

```bash
# 1. Get Function URL
az functionapp function show \
  --name petstore-orderitems-prod \
  --resource-group petstore-rg \
  --function-name reserveOrderItems \
  --query invokeUrlTemplate

# 2. Get Function Key
az functionapp keys list \
  --name petstore-orderitems-prod \
  --resource-group petstore-rg

# 3. Test the function
curl -X POST "https://petstore-orderitems-prod.azurewebsites.net/api/orderitems/reserve?code=YOUR_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "test-123",
    "username": "test",
    "products": [{"id":"1","name":"Test","quantity":1,"price":9.99}]
  }'
```

---

## Troubleshooting

### Deployment fails: "Function App name already exists"
The name must be globally unique. Change `functionAppName` to something unique:
```bash
mvn clean package azure-functions:deploy \
  -DfunctionAppName=petstore-orderitems-$(date +%s)
```

### Function returns 500 after deployment
Check Application Insights or logs:
```bash
# Stream logs
az webapp log tail --name petstore-orderitems-prod --resource-group petstore-rg

# Check if storage connection is configured
az functionapp config appsettings list \
  --name petstore-orderitems-prod \
  --resource-group petstore-rg \
  --query "[?name=='AZURE_STORAGE_CONNECTION_STRING']"
```

### Cannot find Function App after deployment
```bash
# List all function apps in resource group
az functionapp list --resource-group petstore-rg --output table
```

---

## Recommendation for This Project

For this educational/learning project, I recommend:

**Local Development:**
```bash
mvn azure-functions:run
```

**First Azure Deployment:**
```bash
# Use defaults from pom.xml
mvn clean package azure-functions:deploy
```

**If you need multiple environments later:**
```bash
# Override via command line
mvn clean package azure-functions:deploy \
  -DfunctionAppName=your-unique-name \
  -DfunctionResourceGroup=your-rg
```

This gives you flexibility without over-complicating the setup! 🚀
