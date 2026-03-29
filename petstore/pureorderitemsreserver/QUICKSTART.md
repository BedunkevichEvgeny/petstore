# Pure Order Items Reserver - Quick Start Guide

Get the Pure Azure Functions Order Items Reserver service running locally in 5 minutes.

## Prerequisites

- Java 21 installed: `java --version`
- Maven installed: `mvn --version`
- Azure Functions Core Tools: `func --version` (should be 4.x)
- Docker (for Azurite local storage): `docker --version`

## Step 1: Start Azurite (Local Blob Storage)

```bash
# Start Azurite using Docker
docker run -d -p 10000:10000 -p 10001:10001 -p 10002:10002 \
  --name azurite \
  mcr.microsoft.com/azure-storage/azurite
```

Verify it's running:
```bash
docker ps | grep azurite
```

## Step 2: Configure Local Settings

```bash
cd petstore/pureorderitemsreserver
cp local.settings.json.sample local.settings.json
```

The default settings work with Azurite:
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

## Step 3: Build the Project

```bash
mvn clean package
```

Expected output:
```
[INFO] BUILD SUCCESS
[INFO] 2 Azure Functions entry point(s) found.
```

## Step 4: Run the Function Locally

```bash
mvn azure-functions:run
```

Wait for:
```
Functions:
    hello: [GET] http://localhost:7071/api/hello
    reserveOrderItems: [POST] http://localhost:7071/api/orderitems/reserve
```

## Step 5: Test the Functions

### Test Hello Function

```bash
curl http://localhost:7071/api/hello
```

Expected response:
```
hi
```

### Test Order Reservation Function

```bash
curl -X POST http://localhost:7071/api/orderitems/reserve \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "quick-test-001",
    "username": "john.doe",
    "userId": "user-123",
    "products": [
      {
        "id": "1",
        "name": "Dog Food Premium",
        "quantity": 2,
        "price": 29.99
      },
      {
        "id": "2",
        "name": "Cat Toy Mouse",
        "quantity": 1,
        "price": 9.99
      }
    ]
  }'
```

Expected response:
```json
{
  "sessionId": "quick-test-001",
  "status": "SUCCESS",
  "message": "Order items reserved successfully"
}
```

## Step 6: Verify Blob Storage

The order data is now in Azurite blob storage. You can view it using Azure Storage Explorer or the Azure CLI.

### Option A: Azure Storage Explorer
1. Download from: https://azure.microsoft.com/features/storage-explorer/
2. Connect to local emulator
3. Browse: `order-items` container → `quick-test-001.json`

### Option B: Azure CLI
```bash
# List blobs in the container
az storage blob list \
  --account-name devstoreaccount1 \
  --container-name order-items \
  --connection-string "UseDevelopmentStorage=true" \
  --output table

# Download the blob
az storage blob download \
  --account-name devstoreaccount1 \
  --container-name order-items \
  --name "quick-test-001.json" \
  --file order-data.json \
  --connection-string "UseDevelopmentStorage=true"

# View the contents
cat order-data.json
```

Expected blob content:
```json
{
  "sessionId": "quick-test-001",
  "username": "john.doe",
  "userId": "user-123",
  "products": [
    {
      "id": "1",
      "name": "Dog Food Premium",
      "quantity": 2,
      "price": 29.99
    },
    {
      "id": "2",
      "name": "Cat Toy Mouse",
      "quantity": 1,
      "price": 9.99
    }
  ],
  "timestamp": "2026-03-29T13:45:30",
  "status": "RESERVED"
}
```

## Troubleshooting

### Issue: "Cannot find or load main class"
**Solution:** Rebuild the project
```bash
mvn clean package
```

### Issue: "Connection refused" when calling function
**Solution:** Ensure Azurite is running
```bash
docker ps | grep azurite
# If not running:
docker start azurite
```

### Issue: "AZURE_STORAGE_CONNECTION_STRING not set"
**Solution:** Check `local.settings.json` exists
```bash
ls -la local.settings.json
# If missing:
cp local.settings.json.sample local.settings.json
```

### Issue: Function starts but returns 500 error
**Solution:** Check Azure Functions Core Tools version
```bash
func --version  # Should be 4.x
# Update if needed:
npm install -g azure-functions-core-tools@4
```

### Issue: Port 7071 already in use
**Solution:** Kill existing process or use different port
```bash
# Find process using port 7071
lsof -ti:7071 | xargs kill -9
# OR change port in host.json
```

## Testing Multiple Orders

Create a test script `test-orders.sh`:

```bash
#!/bin/bash

# Test multiple sessions
for i in {1..5}; do
  echo "Creating order session-$i..."
  curl -X POST http://localhost:7071/api/orderitems/reserve \
    -H "Content-Type: application/json" \
    -d "{
      \"sessionId\": \"session-$i\",
      \"username\": \"user$i\",
      \"userId\": \"id-$i\",
      \"products\": [
        {
          \"id\": \"1\",
          \"name\": \"Product $i\",
          \"quantity\": $i,
          \"price\": $(echo "scale=2; $i * 10" | bc)
        }
      ]
    }"
  echo -e "\n---"
  sleep 1
done

echo "All orders created!"
```

Run it:
```bash
chmod +x test-orders.sh
./test-orders.sh
```

## Performance Testing

### Load Test with K6 (Optional)

Install K6: https://k6.io/docs/getting-started/installation/

Create `load-test.js`:
```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
  stages: [
    { duration: '30s', target: 10 },  // Ramp up to 10 users
    { duration: '1m', target: 10 },   // Stay at 10 users
    { duration: '30s', target: 0 },   // Ramp down to 0
  ],
};

export default function () {
  const url = 'http://localhost:7071/api/orderitems/reserve';
  const payload = JSON.stringify({
    sessionId: `load-test-${__VU}-${__ITER}`,
    username: `user${__VU}`,
    userId: `id-${__VU}`,
    products: [
      {
        id: '1',
        name: 'Test Product',
        quantity: 1,
        price: 19.99
      }
    ]
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  let res = http.post(url, payload, params);
  check(res, {
    'status is 200': (r) => r.status === 200,
    'response has sessionId': (r) => JSON.parse(r.body).sessionId !== undefined,
  });

  sleep(1);
}
```

Run the load test:
```bash
k6 run load-test.js
```

## Next Steps

1. ✅ **Deploy to Azure**: See [DEPLOYMENT.md](./DEPLOYMENT.md)
2. ✅ **Compare with Spring Boot**: See [COMPARISON.md](./COMPARISON.md)
3. ✅ **Add monitoring**: Configure Application Insights
4. ✅ **Set up CI/CD**: Add GitHub Actions workflow
5. ✅ **Integrate with OrderService**: Update OrderService to use this function

## Clean Up

Stop and remove Azurite:
```bash
docker stop azurite
docker rm azurite
```

Stop the function:
```
Ctrl+C in the terminal running mvn azure-functions:run
```

## Documentation

- [README.md](./README.md) - Full documentation
- [DEPLOYMENT.md](./DEPLOYMENT.md) - Deployment guide
- [COMPARISON.md](./COMPARISON.md) - Spring Boot vs Pure comparison
- [QUICKSTART.md](./QUICKSTART.md) - This file

## Help

If you encounter issues:
1. Check [Troubleshooting](#troubleshooting) section above
2. Review function logs in the terminal
3. Verify Azurite is running: `docker ps`
4. Check Java version: `java --version` (must be 21)
5. Verify Maven can build: `mvn clean package`

## Success Indicators

You're ready to proceed if:
- ✅ Build succeeds without errors
- ✅ Functions start and show endpoints
- ✅ Hello function returns "hi"
- ✅ Order reservation returns success JSON
- ✅ Blob appears in Azurite storage

Time to complete: **~5 minutes** ⚡
