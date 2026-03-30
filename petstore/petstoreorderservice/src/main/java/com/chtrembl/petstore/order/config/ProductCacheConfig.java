package com.chtrembl.petstore.order.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration for product data fetched from Product Service
 * Orders are persisted in Cosmos DB and do not use caching
 */
@Configuration
@EnableCaching
public class ProductCacheConfig {

    @Bean(name = "cacheManager")
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("products");
    }
}
