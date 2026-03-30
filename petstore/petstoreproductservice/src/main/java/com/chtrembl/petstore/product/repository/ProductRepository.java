package com.chtrembl.petstore.product.repository;

import com.chtrembl.petstore.product.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Find products by status enum values.
     * Use Status enum directly for type-safe queries.
     */
    List<Product> findByStatusIn(List<Product.Status> statuses);
}
