package com.chtrembl.petstore.order.config;

import com.azure.cosmos.CosmosClientBuilder;
import com.azure.spring.data.cosmos.config.AbstractCosmosConfiguration;
import com.azure.spring.data.cosmos.config.CosmosConfig;
import com.azure.spring.data.cosmos.repository.config.EnableCosmosRepositories;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Azure Cosmos DB configuration for Order Service
 * Configures connection to Cosmos DB and enables repository support
 */
@Configuration
@EnableCosmosRepositories(basePackages = "com.chtrembl.petstore.order.repository")
public class DBConfig extends AbstractCosmosConfiguration {

    @Value("${azure.cosmos.uri}")
    private String cosmosUri;

    @Value("${azure.cosmos.key}")
    private String cosmosKey;

    @Value("${azure.cosmos.database}")
    private String databaseName;

    @Bean
    public CosmosClientBuilder cosmosClientBuilder() {
        return new CosmosClientBuilder()
                .endpoint(cosmosUri)
                .key(cosmosKey);
    }

    @Bean
    public CosmosConfig cosmosConfig() {
        return CosmosConfig.builder()
                .enableQueryMetrics(true)
                .build();
    }

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }
}
