# Multi-stage Dockerfile for Azure Functions Custom Container
# Optimized for production deployment

# Stage 1: Build the function with Maven
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /build

# Copy pom.xml first for better layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Azure Functions Runtime
FROM mcr.microsoft.com/azure-functions/java:4-java21

# Set environment variables for Azure Functions
ENV AzureWebJobsScriptRoot=/home/site/wwwroot \
    AzureFunctionsJobHost__Logging__Console__IsEnabled=true \
    FUNCTIONS_WORKER_RUNTIME=java

# Copy the built Azure Functions package from build stage
# The azure-functions-maven-plugin creates the package in target/azure-functions/<functionAppName>
COPY --from=build /build/target/azure-functions/yb-pureoreserver-function/ /home/site/wwwroot/

# Expose port 80 (Azure Functions default in containers)
EXPOSE 80

# The base image already has the entrypoint configured
# It will automatically start the Azure Functions host
