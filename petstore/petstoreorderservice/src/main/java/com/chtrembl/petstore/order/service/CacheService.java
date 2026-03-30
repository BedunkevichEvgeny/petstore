package com.chtrembl.petstore.order.service;

import com.chtrembl.petstore.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for retrieving order statistics from Cosmos DB
 * Replaces the cache statistics service after migration to Cosmos DB
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CacheService {

    private final OrderRepository orderRepository;

    /**
     * Gets the total count of orders stored in Cosmos DB
     * @return the number of orders in the database
     */
    public long getOrdersCacheSize() {
        try {
            long count = orderRepository.count();
            log.debug("Total orders in Cosmos DB: {}", count);
            return count;
        } catch (Exception e) {
            log.warn("Could not get orders count from Cosmos DB: {}", e.getMessage());
            return 0;
        }
    }
}
