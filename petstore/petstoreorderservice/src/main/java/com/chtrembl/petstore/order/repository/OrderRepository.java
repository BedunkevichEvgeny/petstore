package com.chtrembl.petstore.order.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import com.chtrembl.petstore.order.model.Order;
import org.springframework.stereotype.Repository;

/**
 * Spring Data Cosmos DB repository for Order entities
 * Provides CRUD operations backed by Azure Cosmos DB
 */
@Repository
public interface OrderRepository extends CosmosRepository<Order, String> {
}
