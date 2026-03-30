# Azure Pet Store - Claude Code Guide

This is a multi-module Maven project for the CloudX Java Azure Dev course, implementing a microservices-based pet store application for Azure cloud development training.

## Project Overview

- **Type**: Microservices-based Spring Boot application (educational project)
- **Build Tool**: Maven 3.9.8, Java 21
- **Core Stack**: Spring Boot 3.5.0, Spring Cloud OpenFeign, Spring Data JPA, Azure Application Insights
- **Database**: PostgreSQL 17 (for Product Service)
- **Deployment**: Designed for Azure App Service with Application Insights monitoring

## Build and Development Commands

### Maven Commands

```bash
# Build all modules from root
mvn clean install

# Build specific service
cd petstore/petstoreapp
mvn clean package

# Run service locally
mvn spring-boot:run

# Skip tests during build
mvn clean package -DskipTests
```

### Docker Compose (Preferred for Local Development)

```bash
cd petstore
cp .env.sample .env  # Configure environment first
docker-compose up -d --build
docker-compose logs -f [service-name]
docker-compose down
```

### Testing

```bash
# Unit tests (when implemented)
mvn test

# Load testing with K6
k6 run tests/BasicScaleTest.js
```

## Architecture and Structure

### Service Architecture

- **PetStoreApp** (port 8080): Frontend web application using Thymeleaf, acts as BFF (Backend for Frontend)
- **PetService** (port 8081): Pet data microservice with Swagger UI
- **ProductService** (port 8082): Product catalog microservice with PostgreSQL 17 backend
- **OrderService** (port 8083): Order management microservice
- **OrderItemsReserver** (port 8084): Azure Function-style HTTP service that reserves order items by uploading JSON to Azure Blob Storage
- **PostgreSQL** (port 5432): PostgreSQL 17 database for Product Service (Docker Compose only)

### Key Architectural Patterns

#### 1. Inter-Service Communication

- Uses **Spring Cloud OpenFeign** for synchronous HTTP calls
- Feign client interfaces located in: `petstoreapp/src/main/java/com/chtrembl/petstoreapp/client/`
- Service URLs configured via environment variables (see `.env.sample`)
- Example pattern:
  ```java
  @FeignClient(name = "petservice", url = "${petstore.service.pet.url}")
  public interface PetServiceClient {
      @GetMapping("/pets")
      List<Pet> getPets();
  }
  ```

#### 2. Session Management

- Session-scoped User bean: `@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)`
- Session-based telemetry tracking with custom properties
- Shopping cart state maintained in session
- Located in: `petstoreapp/src/main/java/com/chtrembl/petstoreapp/model/User.java`

#### 3. Security (Toggle-based)

- Enabled via `PETSTORE_SECURITY_ENABLED` environment variable
- OAuth2 with Microsoft Entra External ID
- Security config in: `petstoreapp/src/main/java/com/chtrembl/petstoreapp/security/`
- When disabled, uses a default test user session

#### 4. Monitoring with Azure Application Insights

- **Custom telemetry wrapper**: `PetStoreTelemetryClient`
- Location: `petstoreapp/src/main/java/com/chtrembl/petstoreapp/telemetry/`
- Tracks custom events, metrics, and exceptions with business context
- Configuration in: `applicationinsights.json`
- **Important**: Use the wrapper instead of direct Application Insights API for consistency
- Custom properties automatically include session context (session ID, user info)

#### 5. Caching

- Spring Cache with Caffeine provider
- Cache named "currentUsers"
- Configured in application configuration classes

#### 6. Database Persistence (Product Service)

- **ORM**: Spring Data JPA with Hibernate
- **Database**: PostgreSQL 17 (Alpine Linux image in Docker)
- **Connection Pool**: HikariCP (Spring Boot default)
- **Entities**: Product, Category, Tag with JPA relationships
  - `Product` has `@ManyToOne` relationship with `Category`
  - `Product` has `@ManyToMany` relationship with `Tag` (via `product_tags` junction table)
- **Repository Pattern**: `ProductRepository extends JpaRepository<Product, Long>`
- **Schema Management**:
  - Hibernate DDL auto mode configured via `JPA_DDL_AUTO` environment variable (default: `update`)
  - Initial data loaded via `src/main/resources/data.sql` with PostgreSQL-specific syntax
  - Uses `ON CONFLICT DO NOTHING` for idempotent data initialization
- **Configuration**:
  - Database connection via environment variables (see `.env.sample`)
  - Docker Compose: Uses container hostname `postgres:5432`
  - Local development: Uses `localhost:5432` or Azure PostgreSQL
- **Location**: `petstore/petstoreproductservice/`
  - Models: `src/main/java/com/chtrembl/petstore/product/model/`
  - Repository: `src/main/java/com/chtrembl/petstore/product/repository/`
  - Service: `src/main/java/com/chtrembl/petstore/product/service/`
  - Data initialization: `src/main/resources/data.sql`

### PetStoreApp Package Structure

```
petstoreapp/src/main/java/com/chtrembl/petstoreapp/
├── client/          # Feign clients for backend services
├── controller/      # MVC controllers (HomeController, ProductController, etc.)
├── service/         # Business logic layer
├── model/           # Domain objects (User, Product, Pet, Order)
├── security/        # OAuth2 security configuration
├── telemetry/       # Application Insights integration
└── config/          # Spring configuration classes
```

### Critical Files for Common Tasks

- **Service URLs**: `petstore/.env` (create from `.env.sample` template)
- **Application Insights config**: `petstore/petstoreapp/src/main/resources/applicationinsights.json`
- **Spring config**: `petstore/petstoreapp/src/main/resources/application.yml`
- **Database config**: `petstore/petstoreproductservice/src/main/resources/application.yml` (PostgreSQL JDBC)
- **Database initialization**: `petstore/petstoreproductservice/src/main/resources/data.sql`
- **Root POM**: `pom.xml` (aggregator for all modules)
- **Version tracking**: Each service has `src/main/resources/version.json`
- **Azure Storage config**: `petstore/orderitemsreserver/src/main/resources/application.yml` (for blob storage)

## Development Workflow Notes

### Working with PostgreSQL Database (Product Service)

**Local Development Setup:**
```bash
# Option 1: Docker Compose (recommended)
cd petstore
cp .env.sample .env  # Configure database credentials
docker-compose up -d postgres  # Start PostgreSQL container
docker-compose up -d petstoreproductservice  # Start product service

# Option 2: Local PostgreSQL installation
# Install PostgreSQL 17 and create database:
createdb petstore
# Update .env with POSTGRES_URL=jdbc:postgresql://localhost:5432/petstore
```

**Database Access:**
```bash
# Connect to PostgreSQL in Docker
docker-compose exec postgres psql -U postgres -d petstore

# Verify tables and data
\dt  # List tables: products, categories, tags, product_tags
SELECT * FROM products;
SELECT * FROM categories;
```

**Environment Variables:**
- `POSTGRES_URL`: JDBC connection string (default: `jdbc:postgresql://localhost:5432/petstore`)
- `POSTGRES_USER`: Database username (default: `postgres`)
- `POSTGRES_PASSWORD`: Database password (default: `postgres`)
- `JPA_DDL_AUTO`: Hibernate DDL mode - `update`, `create`, `create-drop`, `validate` (default: `update`)
- `JPA_SHOW_SQL`: Enable SQL logging for debugging (default: `false`)
- `SQL_INIT_MODE`: Control data.sql execution - `always`, `never`, `embedded` (default: `always`)

**Working with JPA Entities:**
- All entities use `@GeneratedValue(strategy = GenerationType.IDENTITY)` for auto-increment IDs
- Product-Category relationship: `@ManyToOne` with eager fetching
- Product-Tag relationship: `@ManyToMany` via `product_tags` junction table
- Status enum stored as `@Enumerated(EnumType.STRING)` in database

**Adding New Products:**
- Use JPA repository methods: `productRepository.save(product)`
- Or add to `data.sql` for initial seed data
- Ensure foreign key constraints are satisfied (category and tags must exist)

**Azure Deployment:**
- Use Azure Database for PostgreSQL Flexible Server
- Update connection string in Azure App Service configuration
- Set `JPA_DDL_AUTO=validate` in production (never `update` or `create`)
- Consider using Azure Key Vault for database credentials

### Adding New Endpoints

1. **Backend service**: Create `@RestController` in the relevant service (PetService/ProductService/OrderService)
2. **Frontend integration**: Add Feign client interface in `petstoreapp/client/`
3. **Wire to UI**: Update controller and Thymeleaf template in `petstoreapp/controller/` and `resources/templates/`

### Working with Telemetry

- Always use `PetStoreTelemetryClient` wrapper instead of direct Application Insights API
- Access via `@Autowired` in controllers/services
- Key methods:
  - `trackEvent(String name, Map<String, String> properties)`
  - `trackMetric(String name, double value)`
  - `trackException(Exception exception)`
- Custom properties automatically include session context
- Example usage pattern found in controllers for tracking user actions

### Configuration Priority

1. Environment variables (Docker Compose, Azure App Service)
2. `application.yml` defaults
3. Spring Boot conventions

### Common Patterns in This Codebase

- Controllers return view names (Thymeleaf templates in `resources/templates/`)
- Models passed via `Model` parameter in controller methods
- User session accessed via `@ModelAttribute("user") User user`
- Service layer handles business logic and Feign client calls
- Exception handling often includes telemetry tracking

## Health Checks and Debugging

```bash
# Application health
curl http://localhost:8080/actuator/health

# Service-specific health
curl http://localhost:8081/actuator/health  # PetService
curl http://localhost:8082/actuator/health  # ProductService (includes database health)
curl http://localhost:8083/actuator/health  # OrderService
curl http://localhost:8084/actuator/health  # OrderItemsReserver

# Database connectivity check
docker-compose exec postgres pg_isready -U postgres

# Swagger UI (backend services)
# PetService: http://localhost:8081/swagger-ui/
# ProductService: http://localhost:8082/swagger-ui/
# OrderService: http://localhost:8083/swagger-ui/
# OrderItemsReserver: http://localhost:8084/swagger-ui/
```

### Docker Logs

```bash
# View logs for specific service
docker-compose -f petstore/docker-compose.yml logs -f petstoreapp

# View all service logs
docker-compose -f petstore/docker-compose.yml logs -f
```

## Module Structure

The project uses Maven's multi-module structure with the following modules:

- **petstoreapp**: Main web application (BFF)
- **petstoreservice**: Backend services parent
  - **petstorepetservice**: Pet data service
  - **petstoreproductservice**: Product catalog service
  - **petstoreorderservice**: Order management service
  - **petstoreorderitemsreserver**: Order items reservation service

Each module has its own `pom.xml` inheriting from the root POM.

## Important Notes for AI Assistants

- This is an **educational project** - focus on clarity and Azure integration patterns
- **Application Insights integration** is a key learning objective - maintain telemetry tracking
- **Azure Blob Storage integration** in OrderItemsReserver demonstrates serverless patterns
- Environment variable configuration is critical for multi-environment deployment
- Security can be toggled for local development vs. Azure deployment
- The project demonstrates cloud-native patterns (externalized config, health checks, distributed tracing, blob storage)

### OrderItemsReserver Service Notes

- **Type**: Native Azure Function (not Spring Boot REST API)
- **Purpose**: HTTP-triggered function for order reservation
- **Pattern**: Receives order details (sessionId, username, products) and uploads JSON to Azure Blob Storage
- **Implementation**: Uses `@FunctionName` and `@HttpTrigger` annotations
- **Blob naming**: Uses sessionId as filename (e.g., `session-123.json`), overwrites if exists
- **Configuration**: Reads from environment variables (`AZURE_STORAGE_CONNECTION_STRING`)
- **Local testing**:
  - Run with Azure Functions Core Tools: `mvn azure-functions:run`
  - Local endpoint: `http://localhost:7071/api/orderitems/reserve`
  - Use Azurite emulator for blob storage
- **Deployment**: Use `mvn azure-functions:deploy` to deploy to Azure
- **API endpoint**: `POST /api/orderitems/reserve` returns sessionId on success
- **Authorization**: Function-level authorization (requires function key in Azure)

### OrderService → OrderItemsReserver Integration

- **Integration Point**: `OrderController.placeOrder()` method in petstoreorderservice
- **Trigger**: When OrderService receives a complete order (complete=true) from PetStoreApp
- **Feign Client**: `OrderItemsReserverClient` in petstoreorderservice handles HTTP communication
- **DTOs**: `OrderItemsRequest` and `OrderItemsResponse` in petstoreorderservice package
- **Data Flow**:
  1. User clicks "Complete Order" in PetStoreApp UI
  2. PetStoreApp calls OrderService REST API: `POST /petstoreorderservice/v2/store/order`
  3. OrderService processes and enriches the order
  4. If order is complete, OrderService converts Order to OrderItemsRequest
  5. OrderService calls Azure Function via Feign client
  6. Azure Function uploads JSON to Azure Blob Storage
  7. OrderService returns enriched order (non-blocking - proceeds even if function fails)
- **Configuration**: `ORDERITEMSRESERVER_URL` environment variable in OrderService
  - Local: `http://localhost:7071` (Azure Functions Core Tools)
  - Docker: `http://orderitemsreserver:8084`
  - Azure: `https://your-function-app.azurewebsites.net`
- **Architecture**: Microservice pattern - order reservation logic in OrderService, not frontend
- **Testing Guide**: See `INTEGRATION_GUIDE.md` for detailed testing scenarios
